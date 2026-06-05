package com.festapp.dashboard.telemetry.dto;

import com.festapp.dashboard.telemetry.entity.SensorNumericHistory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SensorNumericHistoryResponse {

    private Long historyId;
    private Long sensorId;
    private String sensorName;
    private Double value;
    private String dataType;
    private String unit;
    private LocalDateTime timestamp;

    public static SensorNumericHistoryResponse fromEntity(SensorNumericHistory history) {
        return SensorNumericHistoryResponse.builder()
                .historyId(history.getSensorNumericHistoryId())
                .sensorId(history.getSensor().getSensorId())
                .sensorName(history.getSensor().getSensorName())
                .value(history.getValue())
                .dataType(history.getDataType())
                .unit(history.getUnit())
                .timestamp(history.getTimestamp())
                .build();
    }
}
