package com.festapp.dashboard.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "대시보드 생성/수정 요청")
public class DashboardRequest {

    @NotBlank(message = "Dashboard name은 필수입니다")
    @Size(max = 255, message = "Dashboard name은 255자 이하여야 합니다")
    @Schema(description = "대시보드 이름", example = "Main Dashboard")
    private String dashboardName;

    @Size(max = 1000, message = "Description은 1000자 이하여야 합니다")
    @Schema(description = "대시보드 설명", example = "Primary dashboard for fab monitoring")
    private String description;
}
