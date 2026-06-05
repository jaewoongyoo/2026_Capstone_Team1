package com.festapp.dashboard.equipment.dto;

import com.festapp.dashboard.telemetry.dto.SensorResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiscoveryApplyResponse {

    private Long dashboardId;
    private int equipmentCount;
    private int sensorCount;
    private List<AppliedEquipment> equipment;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AppliedEquipment {
        private EquipmentResponse equipment;
        private List<SensorResponse> sensors;
    }
}
