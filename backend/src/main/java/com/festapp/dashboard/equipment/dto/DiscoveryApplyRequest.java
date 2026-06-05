package com.festapp.dashboard.equipment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "자동 검색 결과 일괄 등록 요청")
public class DiscoveryApplyRequest {

    @NotNull(message = "Dashboard ID는 필수입니다")
    @Schema(description = "등록할 대시보드 ID", example = "2")
    private Long dashboardId;

    @Valid
    @NotEmpty(message = "등록할 장비 목록은 비어 있을 수 없습니다")
    @Schema(description = "자동 검색으로 발견된 장비 목록")
    private List<DiscoveredEquipment> assets;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "자동 검색으로 발견된 장비")
    public static class DiscoveredEquipment {

        @NotBlank(message = "Equipment name은 필수입니다")
        @Size(max = 255, message = "Equipment name은 255자 이하여야 합니다")
        @Schema(description = "장비 이름", example = "CVD-CHAMBER-01")
        private String equipmentName;

        @Size(max = 255, message = "Field는 255자 이하여야 합니다")
        @Schema(description = "장비 분류/필드", example = "CVD")
        private String field;

        @Valid
        @Schema(description = "장비 하위 태그/센서 목록")
        private List<DiscoveredTag> tags;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "자동 검색으로 발견된 태그/센서")
    public static class DiscoveredTag {

        @NotBlank(message = "Sensor name은 필수입니다")
        @Size(max = 255, message = "Sensor name은 255자 이하여야 합니다")
        @Schema(description = "센서/태그 이름", example = "Temp_0")
        private String sensorName;
    }
}
