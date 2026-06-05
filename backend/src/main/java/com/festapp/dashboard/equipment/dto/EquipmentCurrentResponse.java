package com.festapp.dashboard.equipment.dto;

import com.festapp.dashboard.equipment.entity.Equipment;
import com.festapp.dashboard.telemetry.dto.SensorDataPayload;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EquipmentCurrentResponse {

    private Long equipmentId;
    private String equipmentName;
    private String field;
    private Long dashboardId;
    private SensorDataPayload current;

    public static EquipmentCurrentResponse fromEntity(Equipment equipment, SensorDataPayload current) {
        return EquipmentCurrentResponse.builder()
                .equipmentId(equipment.getEquipmentId())
                .equipmentName(equipment.getEquipmentName())
                .field(equipment.getField())
                .dashboardId(equipment.getDashboard().getDashboardId())
                .current(current)
                .build();
    }
}
