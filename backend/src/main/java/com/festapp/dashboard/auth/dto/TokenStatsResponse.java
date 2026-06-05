// 토큰 통계 응답 DTO - 사용자의 토큰 통계 정보
package com.festapp.dashboard.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 토큰 통계 응답 DTO
 *
 * 사용자의 활성 토큰, 만료된 토큰 등의 통계를 반환합니다.
 *
 * @author DashBoar Team
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "토큰 통계 객체")
public class TokenStatsResponse {

    @Schema(description = "사용자 ID", example = "1")
    private Long userId;

    @Schema(description = "활성 토큰 개수", example = "3")
    private long activeTokenCount;

    @Schema(description = "만료된 토큰 개수", example = "5")
    private long expiredTokenCount;

    @Schema(description = "폐지된 토큰 개수", example = "2")
    private long revokedTokenCount;

    @Schema(description = "전체 토큰 개수", example = "10")
    private long totalTokenCount;

    @Schema(description = "가장 최근 토큰 생성 시간")
    private LocalDateTime lastTokenCreatedAt;

    @Schema(description = "가장 최근 로그인 시간")
    private LocalDateTime lastLoginAt;

    /**
     * 토큰 통계 생성
     */
    public static TokenStatsResponse of(
            Long userId,
            long activeTokenCount,
            long expiredTokenCount,
            long revokedTokenCount
    ) {
        return TokenStatsResponse.builder()
                .userId(userId)
                .activeTokenCount(activeTokenCount)
                .expiredTokenCount(expiredTokenCount)
                .revokedTokenCount(revokedTokenCount)
                .totalTokenCount(activeTokenCount + expiredTokenCount + revokedTokenCount)
                .build();
    }
}

