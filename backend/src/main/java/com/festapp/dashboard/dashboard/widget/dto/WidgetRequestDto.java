package com.festapp.dashboard.dashboard.widget.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
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
@Builder(toBuilder = true)
@Schema(description = "위젯 생성/수정 요청 DTO")
public class WidgetRequestDto {

    @Schema(description = "대시보드 ID. 없으면 사용자의 기본 대시보드를 사용합니다", example = "1")
    private Long dashboardId;

    @Size(max = 255, message = "Equipment ID는 255자 이하여야 합니다")
    @Schema(description = "레거시 장비 식별자(장비명 기반). 새 프론트는 equipmentEntityId 사용 권장", example = "CVD-CHAMBER-01")
    private String equipmentId;

    @Schema(description = "장비 엔티티 ID", example = "10")
    private Long equipmentEntityId;

    @NotBlank(message = "Widget type은 필수입니다")
    @Size(max = 50, message = "Widget type은 50자 이하여야 합니다")
    @Schema(description = "위젯 타입", example = "GAUGE")
    private String widgetType;

    @NotBlank(message = "Title은 필수입니다")
    @Size(max = 255, message = "Title은 255자 이하여야 합니다")
    @Schema(description = "위젯 제목", example = "Temperature Gauge")
    private String title;

    @Size(max = 255, message = "Sensor ID는 255자 이하여야 합니다")
    @Schema(description = "레거시 센서 식별자(센서명 기반). 새 프론트는 sensorEntityId 사용 권장", example = "Temp_Sensor_001")
    private String sensorId;

    @Schema(description = "센서 엔티티 ID", example = "100")
    private Long sensorEntityId;

    @Size(max = 50, message = "Chart type은 50자 이하여야 합니다")
    @Schema(description = "차트 타입", example = "line")
    private String chartType;

    @Size(max = 50, message = "Data type은 50자 이하여야 합니다")
    @Schema(description = "데이터 타입", example = "FLOAT")
    private String dataType;

    @Size(max = 50, message = "Unit은 50자 이하여야 합니다")
    @Schema(description = "단위", example = "C")
    private String unit;

    @NotNull(message = "Position X는 필수입니다")
    @Min(value = 0, message = "Position X는 0 이상이어야 합니다")
    @Schema(description = "X 위치", example = "0")
    private Integer posX;

    @NotNull(message = "Position Y는 필수입니다")
    @Min(value = 0, message = "Position Y는 0 이상이어야 합니다")
    @Schema(description = "Y 위치", example = "0")
    private Integer posY;

    @NotNull(message = "Width는 필수입니다")
    @Min(value = 1, message = "Width는 1 이상이어야 합니다")
    @Schema(description = "너비", example = "4")
    private Integer width;

    @NotNull(message = "Height는 필수입니다")
    @Min(value = 1, message = "Height는 1 이상이어야 합니다")
    @Schema(description = "높이", example = "3")
    private Integer height;

    @Schema(description = "추가 설정 정보", example = "{\"refreshInterval\":5000}")
    private String configJson;

    @AssertTrue(message = "equipmentId 또는 equipmentEntityId 중 하나는 필수입니다")
    public boolean hasEquipmentReference() {
        return (equipmentId != null && !equipmentId.isBlank()) || equipmentEntityId != null;
    }
}
