package com.festapp.dashboard.telemetry.dto;

import com.festapp.dashboard.telemetry.entity.SensorStringHistory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SensorStringHistoryResponse {

    private Long historyId;
    private Long sensorId;
    private String sensorName;
    private String status;
    private LocalDateTime timestamp;

    public static SensorStringHistoryResponse fromEntity(SensorStringHistory history) {
        return SensorStringHistoryResponse.builder()
                .historyId(history.getSensorStringHistoryId())
                .sensorId(history.getSensor().getSensorId())
                .sensorName(history.getSensor().getSensorName())
                .status(history.getStatus())
                .timestamp(history.getTimestamp())
                .build();
    }
}
