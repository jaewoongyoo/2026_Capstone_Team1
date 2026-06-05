package com.festapp.dashboard.telemetry.dto;

import com.festapp.dashboard.telemetry.entity.Sensor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SensorResponse {

    private Long sensorId;
    private String sensorName;
    private Long equipmentId;

    public static SensorResponse fromEntity(Sensor sensor) {
        return SensorResponse.builder()
                .sensorId(sensor.getSensorId())
                .sensorName(sensor.getSensorName())
                .equipmentId(sensor.getEquipment().getEquipmentId())
                .build();
    }
}
