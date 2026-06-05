// API 응답 개선 - Correlation ID 포함
package com.festapp.dashboard.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;

/**
 * API 응답 표준 형식 (v2 - Correlation ID 포함)
 *
 * @param <T> 응답 데이터의 타입
 * @author DashBoar Team
 * @version 2.0.0
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "API 응답 객체 (v2 - Correlation ID 포함)")
public class ApiResponseV2<T> {

    @Schema(description = "요청 성공 여부", example = "true")
    private boolean success;

    @Schema(description = "응답 메시지", example = "요청이 성공적으로 처리되었습니다")
    private String message;

    @Schema(description = "응답 데이터")
    private T data;

    @Schema(description = "HTTP 상태 코드", example = "200")
    private Integer statusCode;

    @Schema(description = "에러 코드 (오류 발생시에만 포함)", example = "4001")
    private Integer errorCode;

    @Schema(description = "오류 상세 메시지 (오류 발생시에만 포함)")
    private String errorDetail;

    @Schema(description = "응답 생성 시간", example = "2026-04-03T10:30:00")
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    @Schema(description = "요청 경로 (개발자 참고용)")
    private String path;

    @Schema(description = "Correlation ID (요청 추적용)", example = "550e8400-e29b-41d4-a716-446655440000")
    private String correlationId;

    @Schema(description = "응답 시간 (ms)", example = "123")
    private Long responseTime;

    // 성공 응답 생성
    public static <T> ApiResponseV2<T> success(String message, T data, String correlationId, Long responseTime) {
        return ApiResponseV2.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .statusCode(200)
                .correlationId(correlationId)
                .responseTime(responseTime)
                .timestamp(LocalDateTime.now())
                .build();
    }

    // 오류 응답 생성
    public static <T> ApiResponseV2<T> error(String message, Integer errorCode, String errorDetail,
                                              int statusCode, String correlationId, Long responseTime) {
        return ApiResponseV2.<T>builder()
                .success(false)
                .message(message)
                .errorCode(errorCode)
                .errorDetail(errorDetail)
                .statusCode(statusCode)
                .correlationId(correlationId)
                .responseTime(responseTime)
                .timestamp(LocalDateTime.now())
                .build();
    }
}

