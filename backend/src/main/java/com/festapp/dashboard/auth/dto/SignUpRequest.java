// 회원가입 요청 DTO - 클라이언트로부터 받은 회원가입 정보
package com.festapp.dashboard.auth.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SignUpRequest {
    @NotBlank(message = "Username은 필수입니다")
    @Size(min = 3, max = 50, message = "Username은 3자 이상 50자 이하여야 합니다")
    @Pattern(regexp = "^[a-z0-9_-]+$", message = "Username은 영문 소문자, 숫자, _, -만 사용 가능합니다")
    private String username;

    @NotBlank(message = "Email은 필수입니다")
    @Email(message = "유효한 이메일 형식이어야 합니다")
    @Pattern(regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$", message = "유효한 이메일 형식이어야 합니다")
    @Size(max = 100, message = "Email은 100자 이하여야 합니다")
    private String email;

    @NotBlank(message = "Password는 필수입니다")
    @Size(min = 8, max = 100, message = "Password는 8자 이상 100자 이하여야 합니다")
    @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>?/`~])[a-zA-Z\\d!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>?/`~]{8,}$",
            message = "Password는 영문, 숫자, 특수문자를 포함해야 합니다")
    private String password;

    @Size(max = 100, message = "Full name은 100자 이하여야 합니다")
    private String fullName;
}

