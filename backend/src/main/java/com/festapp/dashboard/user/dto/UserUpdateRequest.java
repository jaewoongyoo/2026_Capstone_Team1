// 사용자 정보 수정 요청 DTO - 클라이언트로부터 받은 사용자 수정 데이터
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
public class UserUpdateRequest {

    @Email(message = "유효한 이메일 형식이어야 합니다")
    @Pattern(regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$", message = "유효한 이메일 형식이어야 합니다")
    @Size(max = 100, message = "Email은 100자 이하여야 합니다")
    private String email;

    @Size(max = 100, message = "전체 이름은 100자 이하여야 합니다")
    private String fullName;
}

