// 비밀번호 변경 요청 DTO - 클라이언트로부터 받은 비밀번호 변경 데이터
package com.festapp.dashboard.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChangePasswordRequest {
    @NotBlank(message = "Current password는 필수입니다")
    private String currentPassword;

    @NotBlank(message = "New password는 필수입니다")
    @Size(min = 8, max = 100, message = "Password는 8자 이상 100자 이하여야 합니다")
    @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>?/`~])[a-zA-Z\\d!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>?/`~]{8,}$",
            message = "Password는 영문, 숫자, 특수문자를 포함해야 합니다")
    private String newPassword;

    @NotBlank(message = "Password confirmation은 필수입니다")
    private String confirmPassword;
}

