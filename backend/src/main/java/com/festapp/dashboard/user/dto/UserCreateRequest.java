// 사용자 생성 요청 DTO - 관리자가 새로운 사용자를 생성할 때 사용
package com.festapp.dashboard.user.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCreateRequest {

    @NotBlank(message = "사용자명은 필수입니다")
    @Size(min = 3, max = 50, message = "사용자명은 3자 이상 50자 이하여야 합니다")
    @Pattern(regexp = "^[a-z0-9_-]+$", message = "사용자명은 영문 소문자, 숫자, _, -만 사용 가능합니다")
    private String username;

    @NotBlank(message = "이메일은 필수입니다")
    @Email(message = "유효한 이메일 형식이어야 합니다")
    @Pattern(regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$", message = "유효한 이메일 형식이어야 합니다")
    @Size(max = 100, message = "이메일은 100자 이하여야 합니다")
    private String email;

    @NotBlank(message = "비밀번호는 필수입니다")
    @Size(min = 8, max = 100, message = "비밀번호는 8자 이상 100자 이하여야 합니다")
    @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*[0-9])(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>?/`~]).+$",
             message = "비밀번호는 영문, 숫자, 특수문자를 포함해야 합니다")
    private String password;

    @NotBlank(message = "전체 이름은 필수입니다")
    @Size(min = 2, max = 100, message = "전체 이름은 2자 이상 100자 이하여야 합니다")
    private String fullName;

    @Builder.Default
    private String role = "USER";
}

