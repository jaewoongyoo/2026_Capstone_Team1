package com.festapp.dashboard.dashboard.dto;

import com.festapp.dashboard.dashboard.entity.Dashboard;
import com.festapp.dashboard.dashboard.widget.dto.WidgetResponseDto;
import com.festapp.dashboard.equipment.dto.EquipmentCurrentResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublicDashboardResponse {
    private Long dashboardId;
    private String dashboardName;
    private String description;
    private String shareToken;
    private Boolean isPublic;
    private List<WidgetResponseDto> widgets;
    private List<EquipmentCurrentResponse> equipmentCurrent;

    public static PublicDashboardResponse of(
            Dashboard dashboard, 
            List<WidgetResponseDto> widgets, 
            List<EquipmentCurrentResponse> equipmentCurrent) {
        return PublicDashboardResponse.builder()
                .dashboardId(dashboard.getDashboardId())
                .dashboardName(dashboard.getDashboardName())
                .description(dashboard.getDescription())
                .shareToken(dashboard.getShareToken())
                .isPublic(dashboard.getIsPublic())
                .widgets(widgets)
                .equipmentCurrent(equipmentCurrent)
                .build();
    }
}
