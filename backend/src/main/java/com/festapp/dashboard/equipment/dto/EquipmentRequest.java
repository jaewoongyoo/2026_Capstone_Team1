package com.festapp.dashboard.equipment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "장비 생성/수정 요청")
public class EquipmentRequest {

    @NotNull(message = "Dashboard ID는 필수입니다")
    @Schema(description = "소속 대시보드 ID", example = "1")
    private Long dashboardId;

    @NotBlank(message = "Equipment name은 필수입니다")
    @Size(max = 255, message = "Equipment name은 255자 이하여야 합니다")
    @Schema(description = "장비 이름", example = "CVD-CHAMBER-01")
    private String equipmentName;

    @Size(max = 255, message = "Field는 255자 이하여야 합니다")
    @Schema(description = "장비 분류/필드", example = "ETCH")
    private String field;
}
