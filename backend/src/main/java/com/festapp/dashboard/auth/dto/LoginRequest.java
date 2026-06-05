// 로그인 요청 DTO - 클라이언트로부터 받은 로그인 정보
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
public class LoginRequest {
    @NotBlank(message = "Username은 필수입니다")
    private String username;

    @NotBlank(message = "Password는 필수입니다")
    private String password;
}

