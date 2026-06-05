// 통계 응답 DTO - 통계 데이터를 반환할 때 사용
package com.festapp.dashboard.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 통계 데이터 응답 DTO
 *
 * 다양한 통계 정보를 표준화된 형식으로 반환합니다.
 *
 * @author DashBoar Team
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "통계 응답 객체")
public class StatisticsResponse {

    @Schema(description = "통계 제목", example = "사용자 통계")
    private String title;

    @Schema(description = "통계 설명", example = "현재 활성화된 사용자 수")
    private String description;

    @Schema(description = "통계값", example = "42")
    private long value;

    @Schema(description = "비교 대상 값", example = "35")
    private Long previousValue;

    @Schema(description = "변화율 (%)", example = "20.0")
    private Double changePercentage;

    @Schema(description = "변화 추세 (UP, DOWN, STABLE)", example = "UP")
    private String trend;

    @Schema(description = "추가 통계 데이터")
    private Map<String, Object> additionalData;

    @Schema(description = "통계 생성 시간")
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    /**
     * 기본 통계 응답 생성
     *
     * @param title 통계 제목
     * @param description 설명
     * @param value 통계값
     * @return StatisticsResponse
     */
    public static StatisticsResponse of(String title, String description, long value) {
        return StatisticsResponse.builder()
                .title(title)
                .description(description)
                .value(value)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * 비교 통계 응답 생성
     *
     * @param title 통계 제목
     * @param description 설명
     * @param value 현재값
     * @param previousValue 이전값
     * @return StatisticsResponse
     */
    public static StatisticsResponse ofWithComparison(String title, String description, long value, long previousValue) {
        long difference = value - previousValue;
        double changePercentage = previousValue > 0 ? (difference * 100.0) / previousValue : 0;
        String trend = difference > 0 ? "UP" : (difference < 0 ? "DOWN" : "STABLE");

        return StatisticsResponse.builder()
                .title(title)
                .description(description)
                .value(value)
                .previousValue(previousValue)
                .changePercentage(changePercentage)
                .trend(trend)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * 추가 데이터와 함께 통계 응답 생성
     *
     * @param title 통계 제목
     * @param description 설명
     * @param value 통계값
     * @param additionalData 추가 데이터
     * @return StatisticsResponse
     */
    public static StatisticsResponse ofWithAdditional(String title, String description, long value, Map<String, Object> additionalData) {
        return StatisticsResponse.builder()
                .title(title)
                .description(description)
                .value(value)
                .additionalData(additionalData)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * 추가 데이터 필드에 값 추가
     *
     * @param key 키
     * @param value 값
     * @return this
     */
    public StatisticsResponse addAdditionalData(String key, Object value) {
        if (this.additionalData == null) {
            this.additionalData = new HashMap<>();
        }
        this.additionalData.put(key, value);
        return this;
    }
}

