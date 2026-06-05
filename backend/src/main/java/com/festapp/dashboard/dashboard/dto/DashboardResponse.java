package com.festapp.dashboard.dashboard.dto;

import com.festapp.dashboard.dashboard.entity.Dashboard;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardResponse {

    private Long dashboardId;
    private String dashboardName;
    private String description;
    private Long userId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String shareToken;
    private Boolean isPublic;

    public static DashboardResponse fromEntity(Dashboard dashboard) {
        return DashboardResponse.builder()
                .dashboardId(dashboard.getDashboardId())
                .dashboardName(dashboard.getDashboardName())
                .description(dashboard.getDescription())
                .userId(dashboard.getUser().getUserId())
                .createdAt(dashboard.getCreatedAt())
                .updatedAt(dashboard.getUpdatedAt())
                .shareToken(dashboard.getShareToken())
                .isPublic(dashboard.getIsPublic())
                .build();
    }
}
