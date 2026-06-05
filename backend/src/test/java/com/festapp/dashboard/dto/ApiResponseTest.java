// API 응답 DTO 테스트
package com.festapp.dashboard.dto;

import com.festapp.dashboard.common.dto.ApiResponse;
import com.festapp.dashboard.common.dto.PaginationResponse;
import com.festapp.dashboard.common.dto.StatisticsResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@DisplayName("API Response DTO 테스트")
public class ApiResponseTest {

    @Test
    @DisplayName("ApiResponse 성공 응답 생성 - 데이터 포함")
    void testSuccessResponseWithData() {
        // Given
        String message = "조회 성공";
        String data = "test data";

        // When
        ApiResponse<String> response = ApiResponse.success(message, data);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).isEqualTo(message);
        assertThat(response.getData()).isEqualTo(data);
        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(response.getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("ApiResponse 성공 응답 생성 - 데이터 없음")
    void testSuccessResponseWithoutData() {
        // Given
        String message = "삭제 성공";

        // When
        ApiResponse<Void> response = ApiResponse.success(message);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).isEqualTo(message);
        assertThat(response.getData()).isNull();
        assertThat(response.getStatusCode()).isEqualTo(200);
    }

    @Test
    @DisplayName("ApiResponse 오류 응답 생성")
    void testErrorResponse() {
        // Given
        String message = "오류 발생";
        int statusCode = 400;

        // When
        ApiResponse<Void> response = ApiResponse.error(message, statusCode);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getMessage()).isEqualTo(message);
        assertThat(response.getStatusCode()).isEqualTo(statusCode);
    }

    @Test
    @DisplayName("ApiResponse 오류 응답 생성 - 에러 코드 포함")
    void testErrorResponseWithErrorCode() {
        // Given
        String message = "입력값 검증 실패";
        Integer errorCode = 4100;
        int statusCode = 400;

        // When
        ApiResponse<Void> response = ApiResponse.error(message, errorCode, statusCode);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getErrorCode()).isEqualTo(errorCode);
        assertThat(response.getStatusCode()).isEqualTo(statusCode);
    }

    @Test
    @DisplayName("PaginationResponse 생성 및 상태 확인")
    void testPaginationResponse() {
        // Given
        PaginationResponse<String> pagination = PaginationResponse.<String>builder()
                .content(Arrays.asList("item1", "item2", "item3"))
                .pageNumber(0)
                .pageSize(10)
                .totalElements(25)
                .totalPages(3)
                .isFirst(true)
                .isLast(false)
                .hasNext(true)
                .hasPrevious(false)
                .build();

        // When & Then
        assertThat(pagination.getContent()).hasSize(3);
        assertThat(pagination.getPageNumber()).isEqualTo(0);
        assertThat(pagination.getPageSize()).isEqualTo(10);
        assertThat(pagination.getTotalElements()).isEqualTo(25);
        assertThat(pagination.getTotalPages()).isEqualTo(3);
        assertThat(pagination.isFirst()).isTrue();
        assertThat(pagination.isLast()).isFalse();
        assertThat(pagination.isHasNext()).isTrue();
        assertThat(pagination.isHasPrevious()).isFalse();
    }

    @Test
    @DisplayName("StatisticsResponse 기본 생성")
    void testStatisticsResponseBasic() {
        // Given
        String title = "활성 사용자";
        String description = "현재 활성화된 사용자 수";
        long value = 42;

        // When
        StatisticsResponse response = StatisticsResponse.of(title, description, value);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getTitle()).isEqualTo(title);
        assertThat(response.getDescription()).isEqualTo(description);
        assertThat(response.getValue()).isEqualTo(value);
        assertThat(response.getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("StatisticsResponse 비교 데이터 포함")
    void testStatisticsResponseWithComparison() {
        // Given
        String title = "활성 사용자";
        String description = "현재 활성화된 사용자 수";
        long currentValue = 50;
        long previousValue = 40;

        // When
        StatisticsResponse response = StatisticsResponse.ofWithComparison(title, description, currentValue, previousValue);

        // Then
        assertThat(response.getValue()).isEqualTo(currentValue);
        assertThat(response.getPreviousValue()).isEqualTo(previousValue);
        assertThat(response.getChangePercentage()).isEqualTo(25.0);
        assertThat(response.getTrend()).isEqualTo("UP");
    }

    @Test
    @DisplayName("StatisticsResponse 추가 데이터 포함")
    void testStatisticsResponseWithAdditionalData() {
        // Given
        Map<String, Object> additionalData = new HashMap<>();
        additionalData.put("region", "Asia");
        additionalData.put("department", "Engineering");

        // When
        StatisticsResponse response = StatisticsResponse.ofWithAdditional(
                "직원 수",
                "부서별 직원 수",
                150,
                additionalData
        );

        // Then
        assertThat(response.getAdditionalData()).isNotNull();
        assertThat(response.getAdditionalData().get("region")).isEqualTo("Asia");
        assertThat(response.getAdditionalData().get("department")).isEqualTo("Engineering");
    }

    @Test
    @DisplayName("StatisticsResponse addAdditionalData 메서드")
    void testStatisticsResponseAddAdditionalData() {
        // Given
        StatisticsResponse response = StatisticsResponse.of("test", "description", 100);

        // When
        response.addAdditionalData("key1", "value1");
        response.addAdditionalData("key2", 42);

        // Then
        assertThat(response.getAdditionalData()).isNotNull();
        assertThat(response.getAdditionalData().get("key1")).isEqualTo("value1");
        assertThat(response.getAdditionalData().get("key2")).isEqualTo(42);
    }
}

