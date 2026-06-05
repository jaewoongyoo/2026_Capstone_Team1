// 위젯 레이아웃 업데이트 DTO - 여러 위젯의 레이아웃을 일괄 업데이트할 데이터
package com.festapp.dashboard.dashboard.widget.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * 위젯 레이아웃 일괄 업데이트 요청 DTO
 *
 * 프론트엔드에서 여러 위젯의 위치와 크기를 동시에 업데이트할 때 사용합니다.
 * 하나의 위젯이라도 실패하면 전체 요청이 실패하고 아무것도 변경되지 않습니다.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "위젯 레이아웃 일괄 업데이트 요청 DTO")
public class WidgetLayoutUpdateDto {

    @Schema(description = "업데이트할 위젯 레이아웃 정보 목록", required = true)
    private List<@Valid LayoutItem> layouts;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "개별 위젯의 레이아웃 정보")
    public static class LayoutItem {
        @NotNull(message = "Widget ID는 필수입니다")
        @Schema(description = "위젯 ID (필수)", example = "1")
        private Long widgetId;

        @NotNull(message = "Position X는 필수입니다")
        @Min(value = 0, message = "Position X는 0 이상이어야 합니다")
        @Schema(description = "X 위치 (필수, 0 이상)", example = "2")
        private Integer posX;

        @NotNull(message = "Position Y는 필수입니다")
        @Min(value = 0, message = "Position Y는 0 이상이어야 합니다")
        @Schema(description = "Y 위치 (필수, 0 이상)", example = "1")
        private Integer posY;

        @NotNull(message = "Width는 필수입니다")
        @Min(value = 1, message = "Width는 1 이상이어야 합니다")
        @Schema(description = "너비 (필수, 1 이상)", example = "4")
        private Integer width;

        @NotNull(message = "Height는 필수입니다")
        @Min(value = 1, message = "Height는 1 이상이어야 합니다")
        @Schema(description = "높이 (필수, 1 이상)", example = "3")
        private Integer height;
    }
}

