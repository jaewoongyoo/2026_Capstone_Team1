package com.festapp.dashboard.telemetry.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.festapp.dashboard.equipment.service.EquipmentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

@DisplayName("MQTT Telemetry Consumer 단위 테스트")
class MqttTelemetryConsumerTest {

    @Test
    @DisplayName("MQTT telemetry topic의 SensorDataPayload JSON을 기존 실시간 처리 흐름으로 전달한다")
    void mqttTelemetryPayloadDelegatesToRealTimeService() {
        RealTimeDataService realTimeDataService = mock(RealTimeDataService.class);
        EquipmentService equipmentService = mock(EquipmentService.class);
        MqttTelemetryConsumer consumer = new MqttTelemetryConsumer(new ObjectMapper(), realTimeDataService, equipmentService);

        consumer.consume("factory/equipment/CVD-CHAMBER-01/telemetry", """
                {
                  "timestamp": "2026-05-13T00:00:00Z",
                  "status": "RUN",
                  "sensors": [
                    {"sensorId": "Temp_0", "value": 971.5, "unit": "C"}
                  ]
                }
                """);

        verify(realTimeDataService).processSensorData(argThat(payload ->
                "CVD-CHAMBER-01".equals(payload.getEquipmentId())
                        && "RUN".equals(payload.getStatus())
                        && payload.getSensors().size() == 1
                        && "Temp_0".equals(payload.getSensors().get(0).getSensorId())
        ));
    }

    @Test
    @DisplayName("MQTT flat telemetry JSON은 센서 목록으로 변환한다")
    void mqttFlatTelemetryPayloadIsConvertedToSensors() {
        RealTimeDataService realTimeDataService = mock(RealTimeDataService.class);
        EquipmentService equipmentService = mock(EquipmentService.class);
        MqttTelemetryConsumer consumer = new MqttTelemetryConsumer(new ObjectMapper(), realTimeDataService, equipmentService);

        consumer.consume("factory/equipment/ETCHER-01/telemetry", """
                {
                  "timestamp": "2026-05-13T00:00:00Z",
                  "status": "RUN",
                  "pressure": 1.25,
                  "doorClosed": true,
                  "mode": "AUTO"
                }
                """);

        verify(realTimeDataService).processSensorData(argThat(payload ->
                "ETCHER-01".equals(payload.getEquipmentId())
                        && payload.getSensors().size() == 3
                        && payload.getSensors().stream().anyMatch(sensor -> "pressure".equals(sensor.getSensorId()))
                        && payload.getSensors().stream().anyMatch(sensor -> "doorClosed".equals(sensor.getSensorId()))
                        && payload.getSensors().stream().anyMatch(sensor -> "mode".equals(sensor.getSensorId()))
        ));
    }

    @Test
    @DisplayName("MQTT metadata topic은 장비/센서를 upsert한다")
    void mqttMetadataPayloadIsDelegated() {
        RealTimeDataService realTimeDataService = mock(RealTimeDataService.class);
        EquipmentService equipmentService = mock(EquipmentService.class);
        MqttTelemetryConsumer consumer = new MqttTelemetryConsumer(new ObjectMapper(), realTimeDataService, equipmentService);

        consumer.consume("factory/equipment/CVD-CHAMBER-01/metadata", """
                {
                  "equipmentName": "CVD-CHAMBER-01",
                  "tags": ["Temp_0", "Pressure_0"]
                }
                """);

        verify(realTimeDataService, never()).processSensorData(any());
        verify(equipmentService, times(1)).upsertEquipmentMetadata(eq("CVD-CHAMBER-01"), argThat(tags -> tags.size() == 2 && tags.contains("Temp_0") && tags.contains("Pressure_0")));
    }

    @Test
    @DisplayName("시뮬레이터 MQTT metadata의 sensors 배열은 센서명 목록으로 변환한다")
    void simulatorMetadataSensorsAreConvertedToTags() {
        RealTimeDataService realTimeDataService = mock(RealTimeDataService.class);
        EquipmentService equipmentService = mock(EquipmentService.class);
        MqttTelemetryConsumer consumer = new MqttTelemetryConsumer(new ObjectMapper(), realTimeDataService, equipmentService);

        consumer.consume("factory/equipment/CVD-CHAMBER-01/metadata", """
                {
                  "equipmentId": "CVD-CHAMBER-01",
                  "equipmentType": "CVD",
                  "sensors": [
                    {"svid": 1, "name": "Chamber_Pressure", "dataType": "FLOAT", "unit": "mTorr"},
                    {"svid": 2, "name": "Heater_Temp_Zone1", "dataType": "FLOAT", "unit": "degC"}
                  ]
                }
                """);

        verify(realTimeDataService, never()).processSensorData(any());
        verify(equipmentService).upsertEquipmentMetadata(eq("CVD-CHAMBER-01"), argThat(tags ->
                tags.size() == 2
                        && tags.contains("Chamber_Pressure")
                        && tags.contains("Heater_Temp_Zone1")));
    }

    @Test
    @DisplayName("지원하지 않는 MQTT topic은 건너뛴다")
    void unsupportedTopicIsSkipped() {
        RealTimeDataService realTimeDataService = mock(RealTimeDataService.class);
        EquipmentService equipmentService = mock(EquipmentService.class);
        MqttTelemetryConsumer consumer = new MqttTelemetryConsumer(new ObjectMapper(), realTimeDataService, equipmentService);

        consumer.consume("factory/unknown/CVD-CHAMBER-01/telemetry", "{\"pressure\":1.25}");

        verify(realTimeDataService, never()).processSensorData(any());
    }

    @Test
    @DisplayName("시뮬레이터 MQTT payload의 name/svid 센서 형식을 표준 센서 형식으로 변환한다")
    void simulatorTelemetryPayloadIsNormalized() {
        RealTimeDataService realTimeDataService = mock(RealTimeDataService.class);
        EquipmentService equipmentService = mock(EquipmentService.class);
        MqttTelemetryConsumer consumer = new MqttTelemetryConsumer(new ObjectMapper(), realTimeDataService, equipmentService);

        consumer.consume("factory/equipment/ETCHER-01/telemetry", """
                {
                  "equipmentId": "ETCHER-01",
                  "timestamp": "2026-05-13T02:16:09.841019+00:00",
                  "status": "COOLDOWN",
                  "sensors": [
                    {"svid": 1, "name": "Chamber_Pressure", "dataType": "FLOAT", "value": 8.03, "unit": "mTorr"},
                    {"svid": 8, "name": "Vacuum_Pump_State", "dataType": "BOOLEAN", "value": 1, "unit": "STATE"},
                    {"svid": 9, "name": "Wafer_Processed_Count", "dataType": "INTEGER", "value": 1, "unit": "ea"},
                    {"svid": 11, "name": "Equipment_State", "dataType": "STRING", "value": "COOLDOWN", "unit": ""}
                  ]
                }
                """);

        verify(realTimeDataService).processSensorData(argThat(payload ->
                "ETCHER-01".equals(payload.getEquipmentId())
                        && "COOLDOWN".equals(payload.getStatus())
                        && payload.getSensors().size() == 4
                        && payload.getSensors().stream().anyMatch(sensor ->
                        "Chamber_Pressure".equals(sensor.getSensorId()) && "FLOAT".equals(sensor.getDataType()))
                        && payload.getSensors().stream().anyMatch(sensor ->
                        "Vacuum_Pump_State".equals(sensor.getSensorId()) && Boolean.TRUE.equals(sensor.getValue()))
                        && payload.getSensors().stream().anyMatch(sensor ->
                        "Wafer_Processed_Count".equals(sensor.getSensorId()) && Long.valueOf(1L).equals(sensor.getValue()))
                        && payload.getSensors().stream().anyMatch(sensor ->
                        "Equipment_State".equals(sensor.getSensorId()) && "COOLDOWN".equals(sensor.getValue()))
        ));
    }
}
