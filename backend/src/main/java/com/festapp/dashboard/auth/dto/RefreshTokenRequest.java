// 토큰 갱신 요청 DTO - 클라이언트로부터 받은 리프레시 토큰
package com.festapp.dashboard.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshTokenRequest {
    @NotBlank(message = "Refresh token은 필수입니다")
    private String refreshToken;
}

