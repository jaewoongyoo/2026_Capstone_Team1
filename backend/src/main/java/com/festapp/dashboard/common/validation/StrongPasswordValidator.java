// 강력한 비밀번호 검증 로직
package com.festapp.dashboard.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * StrongPassword 애노테이션 검증 로직
 *
 * @author DashBoar Team
 * @version 1.0.0
 */
public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {

    private static final String PASSWORD_PATTERN =
        "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[a-zA-Z\\d@$!%*?&]{8,}$";

    @Override
    public void initialize(StrongPassword annotation) {
        // 초기화 로직 (필요시)
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // null은 @NotNull에서 처리
        }
        return value.matches(PASSWORD_PATTERN);
    }
}

