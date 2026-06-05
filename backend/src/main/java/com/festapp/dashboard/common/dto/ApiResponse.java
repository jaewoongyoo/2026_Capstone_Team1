// API 응답 DTO - 모든 API 응답을 통일된 형식으로 반환
package com.festapp.dashboard.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * API 응답 표준 형식
 * 
 * 모든 API 응답은 이 형식을 따릅니다.
 * 성공/실패 여부, 메시지, 데이터를 포함합니다.
 * 
 * @param <T> 응답 데이터의 타입
 * @author DashBoar Team
 * @version 1.0.0
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "API 응답 객체")
public class ApiResponse<T> {
    
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

    /**
     * 성공 응답 생성 (데이터 포함)
     * 
     * @param message 응답 메시지
     * @param data 응답 데이터
     * @return 성공 응답
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .statusCode(200)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * 성공 응답 생성 (데이터 없음)
     * 
     * @param message 응답 메시지
     * @return 성공 응답
     */
    public static <T> ApiResponse<T> success(String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .statusCode(200)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * 성공 응답 생성 (HTTP 상태 코드 지정)
     * 
     * @param message 응답 메시지
     * @param data 응답 데이터
     * @param statusCode HTTP 상태 코드
     * @return 성공 응답
     */
    public static <T> ApiResponse<T> success(String message, T data, int statusCode) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .statusCode(statusCode)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * 오류 응답 생성
     * 
     * @param message 오류 메시지
     * @param statusCode HTTP 상태 코드
     * @return 오류 응답
     */
    public static <T> ApiResponse<T> error(String message, int statusCode) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .statusCode(statusCode)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * 오류 응답 생성 (에러 코드 포함)
     * 
     * @param message 오류 메시지
     * @param errorCode 에러 코드
     * @param statusCode HTTP 상태 코드
     * @return 오류 응답
     */
    public static <T> ApiResponse<T> error(String message, Integer errorCode, int statusCode) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .errorCode(errorCode)
                .statusCode(statusCode)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * 오류 응답 생성 (에러 코드 및 상세 메시지 포함)
     * 
     * @param message 오류 메시지
     * @param errorCode 에러 코드
     * @param errorDetail 오류 상세 메시지
     * @param statusCode HTTP 상태 코드
     * @return 오류 응답
     */
    public static <T> ApiResponse<T> error(String message, Integer errorCode, String errorDetail, int statusCode) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .errorCode(errorCode)
                .errorDetail(errorDetail)
                .statusCode(statusCode)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * 오류 응답 생성 (path 포함)
     */
    public static <T> ApiResponse<T> error(String message, Integer errorCode, String errorDetail, int statusCode, String path) {
        ApiResponse<T> response = ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .errorCode(errorCode)
                .errorDetail(errorDetail)
                .statusCode(statusCode)
                .timestamp(LocalDateTime.now())
                .path(path)
                .build();
        return response;
    }

    /**
     * 실패 응답 생성 (기본값: 400)
     * 
     * @param message 실패 메시지
     * @return 실패 응답
     */
    public static <T> ApiResponse<T> failure(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .statusCode(400)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * 실패 응답 생성 (statusCode 지정)
     */
    public static <T> ApiResponse<T> failure(String message, int statusCode) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .statusCode(statusCode)
                .timestamp(LocalDateTime.now())
                .build();
    }
}

