// WebSocket 메시지 DTO - 위젯 변경 이벤트 정보
package com.festapp.dashboard.dashboard.widget.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "WebSocket 위젯 변경 메시지")
public class WidgetWebSocketMessage {

    @Schema(description = "메시지 타입 (WIDGET_CREATED, WIDGET_UPDATED, WIDGET_DELETED, LAYOUT_UPDATED)")
    private String messageType;

    @Schema(description = "변경된 위젯 ID")
    private Long widgetId;

    @Schema(description = "변경을 수행한 사용자 ID")
    private Long userId;

    @Schema(description = "레거시 장비 식별자(장비명 기반)")
    private String equipmentId;

    @Schema(description = "장비 엔티티 ID")
    private Long equipmentEntityId;

    @Schema(description = "변경된 위젯 정보")
    private WidgetResponseDto widget;

    @Schema(description = "메시지 생성 시간")
    private LocalDateTime timestamp;

    // 메시지 타입 상수
    public static class MessageType {
        public static final String WIDGET_CREATED = "WIDGET_CREATED";
        public static final String WIDGET_UPDATED = "WIDGET_UPDATED";
        public static final String WIDGET_DELETED = "WIDGET_DELETED";
        public static final String LAYOUT_UPDATED = "LAYOUT_UPDATED";
    }
}

