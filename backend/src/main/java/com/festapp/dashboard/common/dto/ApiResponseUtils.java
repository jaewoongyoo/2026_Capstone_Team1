// API 응답 유틸리티 - ApiResponse 생성 및 처리 보조 클래스
package com.festapp.dashboard.common.dto;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;

/**
 * API 응답 생성 유틸리티
 *
 * ApiResponse를 쉽게 생성하고 처리하기 위한 유틸리티 메서드를 제공합니다.
 *
 * @author DashBoar Team
 * @version 1.0.0
 */
public class ApiResponseUtils {

    /**
     * 성공 응답 생성
     *
     * @param message 메시지
     * @param data 데이터
     * @param <T> 데이터 타입
     * @return ResponseEntity
     */
    public static <T> ResponseEntity<ApiResponse<T>> success(String message, T data) {
        return ResponseEntity.ok(ApiResponse.success(message, data));
    }

    /**
     * 성공 응답 생성 (데이터 없음)
     *
     * @param message 메시지
     * @param <T> 데이터 타입
     * @return ResponseEntity
     */
    public static <T> ResponseEntity<ApiResponse<T>> success(String message) {
        return ResponseEntity.ok(ApiResponse.success(message));
    }

    /**
     * 성공 응답 생성 (페이징)
     *
     * @param message 메시지
     * @param page 페이지 객체
     * @param <T> 데이터 타입
     * @return ResponseEntity
     */
    public static <T> ResponseEntity<ApiResponse<PaginationResponse<T>>> successPaginated(String message, Page<T> page) {
        PaginationResponse<T> paginationResponse = PaginationResponse.from(page);
        return ResponseEntity.ok(ApiResponse.success(message, paginationResponse));
    }

    /**
     * 성공 응답 생성 (통계)
     *
     * @param message 메시지
     * @param statistics 통계 데이터
     * @return ResponseEntity
     */
    public static ResponseEntity<ApiResponse<StatisticsResponse>> successStatistics(String message, StatisticsResponse statistics) {
        return ResponseEntity.ok(ApiResponse.success(message, statistics));
    }

    /**
     * 생성 성공 응답 (201 Created)
     *
     * @param message 메시지
     * @param data 생성된 데이터
     * @param <T> 데이터 타입
     * @return ResponseEntity
     */
    public static <T> ResponseEntity<ApiResponse<T>> created(String message, T data) {
        return ResponseEntity.status(201).body(ApiResponse.success(message, data, 201));
    }

    /**
     * 오류 응답 생성
     *
     * @param message 메시지
     * @param errorCode 에러 코드
     * @param statusCode HTTP 상태 코드
     * @param <T> 데이터 타입
     * @return ResponseEntity
     */
    public static <T> ResponseEntity<ApiResponse<T>> error(String message, Integer errorCode, int statusCode) {
        return ResponseEntity.status(statusCode).body(ApiResponse.error(message, errorCode, statusCode));
    }

    /**
     * 오류 응답 생성 (상세 메시지 포함)
     *
     * @param message 메시지
     * @param errorCode 에러 코드
     * @param errorDetail 상세 메시지
     * @param statusCode HTTP 상태 코드
     * @param <T> 데이터 타입
     * @return ResponseEntity
     */
    public static <T> ResponseEntity<ApiResponse<T>> error(String message, Integer errorCode, String errorDetail, int statusCode) {
        return ResponseEntity.status(statusCode).body(ApiResponse.error(message, errorCode, errorDetail, statusCode));
    }

    /**
     * 구성된 Page 객체로부터 페이징 응답 생성
     *
     * @param page Spring Data Page 객체
     * @param content 매핑된 컨텐츠
     * @param <T> 데이터 타입
     * @return PaginationResponse
     */
    public static <T> PaginationResponse<T> paginate(Page<?> page, java.util.List<T> content) {
        return PaginationResponse.from(page, content);
    }

    /**
     * 직접 Page<T> 객체로부터 페이징 응답 생성
     *
     * @param page Spring Data Page<T> 객체
     * @param <T> 데이터 타입
     * @return PaginationResponse
     */
    public static <T> PaginationResponse<T> paginate(Page<T> page) {
        return PaginationResponse.from(page);
    }

    /**
     * 비활성 화 여부 확인
     *
     * @param response ApiResponse 객체
     * @return 성공 여부
     */
    public static boolean isSuccess(ApiResponse<?> response) {
        return response != null && response.isSuccess();
    }

    /**
     * 에러 응답 여부 확인
     *
     * @param response ApiResponse 객체
     * @return 에러 여부
     */
    public static boolean isError(ApiResponse<?> response) {
        return response != null && !response.isSuccess();
    }
}

