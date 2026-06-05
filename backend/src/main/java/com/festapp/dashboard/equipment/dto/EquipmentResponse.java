package com.festapp.dashboard.equipment.dto;

import com.festapp.dashboard.equipment.entity.Equipment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EquipmentResponse {

    private Long equipmentId;
    private String equipmentName;
    private String field;
    private Long dashboardId;

    public static EquipmentResponse fromEntity(Equipment equipment) {
        return EquipmentResponse.builder()
                .equipmentId(equipment.getEquipmentId())
                .equipmentName(equipment.getEquipmentName())
                .field(equipment.getField())
                .dashboardId(equipment.getDashboard().getDashboardId())
                .build();
    }
}
