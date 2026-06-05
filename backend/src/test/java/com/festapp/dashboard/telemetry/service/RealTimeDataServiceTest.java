package com.festapp.dashboard.telemetry.service;

import com.festapp.dashboard.equipment.entity.Equipment;
import com.festapp.dashboard.equipment.repository.EquipmentRepository;
import com.festapp.dashboard.telemetry.dto.SensorDataPayload;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RealTimeDataService 단위 테스트")
class RealTimeDataServiceTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private SensorHistoryService sensorHistoryService;

    @Mock
    private EquipmentRepository equipmentRepository;

    @Mock
    private ObjectProvider<KafkaTelemetryProducer> kafkaTelemetryProducerProvider;

    @Mock
    private KafkaTelemetryProducer kafkaTelemetryProducer;

    @InjectMocks
    private RealTimeDataService realTimeDataService;

    @Test
    @DisplayName("유효한 telemetry는 저장, Redis snapshot, WebSocket 브로드캐스트를 수행한다")
    void processValidTelemetryPersistsSnapshotAndBroadcasts() {
        SensorDataPayload payload = payload(10L, "CVD-01");
        when(sensorHistoryService.persistTelemetryPayload(payload)).thenReturn(true);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        boolean processed = realTimeDataService.processSensorData(payload);

        assertThat(processed).isTrue();
        assertThat(payload.getSensors().get(0).getDataType()).isEqualTo("FLOAT");
        verify(sensorHistoryService).persistTelemetryPayload(payload);
        verify(valueOperations).set("equipment:current:id:10", payload);
        verify(valueOperations).set("equipment:current:CVD-01", payload);
        verify(messagingTemplate).convertAndSend("/topic/equipment/CVD-01", payload);
        verify(messagingTemplate).convertAndSend("/topic/equipment-id/10", payload);
    }

    @Test
    @DisplayName("장비를 찾지 못한 telemetry는 Redis와 WebSocket을 건너뛴다")
    void unresolvedTelemetryDoesNotSnapshotOrBroadcast() {
        SensorDataPayload payload = payload(10L, "WRONG-NAME");
        when(sensorHistoryService.persistTelemetryPayload(payload)).thenReturn(false);

        boolean processed = realTimeDataService.processSensorData(payload);

        assertThat(processed).isFalse();
        verify(sensorHistoryService).persistTelemetryPayload(payload);
        verify(redisTemplate, never()).opsForValue();
        verify(messagingTemplate, never()).convertAndSend(any(String.class), any(Object.class));
    }

    @Test
    @DisplayName("장비 참조가 없는 telemetry는 처리하지 않는다")
    void missingEquipmentReferenceIsSkipped() {
        SensorDataPayload payload = payload(null, null);

        boolean processed = realTimeDataService.processSensorData(payload);

        assertThat(processed).isFalse();
        verify(sensorHistoryService, never()).persistTelemetryPayload(any());
        verify(redisTemplate, never()).opsForValue();
        verify(messagingTemplate, never()).convertAndSend(any(String.class), any(Object.class));
    }

    @Test
    @DisplayName("Redis 오류가 발생해도 WebSocket 브로드캐스트는 계속 수행한다")
    void redisFailureDoesNotBlockBroadcast() {
        SensorDataPayload payload = payload(10L, "CVD-01");
        when(sensorHistoryService.persistTelemetryPayload(payload)).thenReturn(true);
        when(redisTemplate.opsForValue()).thenThrow(new IllegalStateException("redis down"));

        boolean processed = realTimeDataService.processSensorData(payload);

        assertThat(processed).isTrue();
        verify(messagingTemplate).convertAndSend(eq("/topic/equipment/CVD-01"), eq(payload));
        verify(messagingTemplate).convertAndSend(eq("/topic/equipment-id/10"), eq(payload));
    }

    @Test
    @DisplayName("장비명만 있는 telemetry는 같은 이름의 모든 장비 ID snapshot과 topic을 갱신한다")
    void legacyNameOnlyTelemetryUpdatesAllMatchingEquipmentIds() {
        SensorDataPayload payload = payload(null, "CVD-01");
        Equipment first = Equipment.builder().equipmentId(10L).equipmentName("CVD-01").build();
        Equipment second = Equipment.builder().equipmentId(11L).equipmentName("CVD-01").build();
        when(sensorHistoryService.persistTelemetryPayload(payload)).thenReturn(true);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(equipmentRepository.findByEquipmentName("CVD-01")).thenReturn(List.of(first, second));

        boolean processed = realTimeDataService.processSensorData(payload);

        assertThat(processed).isTrue();
        verify(valueOperations).set(eq("equipment:current:CVD-01"), eq(payload));
        verify(valueOperations).set(eq("equipment:current:id:10"), any(SensorDataPayload.class));
        verify(valueOperations).set(eq("equipment:current:id:11"), any(SensorDataPayload.class));
        verify(messagingTemplate).convertAndSend(eq("/topic/equipment/CVD-01"), eq(payload));
        verify(messagingTemplate).convertAndSend(eq("/topic/equipment-id/10"), any(SensorDataPayload.class));
        verify(messagingTemplate).convertAndSend(eq("/topic/equipment-id/11"), any(SensorDataPayload.class));
    }

    @Test
    @DisplayName("Kafka producer가 활성화되어 있으면 처리된 telemetry를 Kafka로 발행한다")
    void processValidTelemetryPublishesToKafkaWhenProducerIsAvailable() {
        SensorDataPayload payload = payload(10L, "CVD-01");
        when(sensorHistoryService.persistTelemetryPayload(payload)).thenReturn(true);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(kafkaTelemetryProducerProvider.getIfAvailable()).thenReturn(kafkaTelemetryProducer);
        ReflectionTestUtils.setField(realTimeDataService, "kafkaSensorTopic", "uemd.sensor.data");

        boolean processed = realTimeDataService.processSensorData(payload);

        assertThat(processed).isTrue();
        verify(kafkaTelemetryProducer).publishTelemetry("uemd.sensor.data", payload);
    }

    private SensorDataPayload payload(Long equipmentEntityId, String equipmentId) {
        return SensorDataPayload.builder()
                .equipmentEntityId(equipmentEntityId)
                .equipmentId(equipmentId)
                .status("RUN")
                .sensors(List.of(
                        SensorDataPayload.SensorDetails.builder()
                                .sensorId("Temp_0")
                                .value(971.5)
                                .unit("C")
                                .build()
                ))
                .build();
    }
}
