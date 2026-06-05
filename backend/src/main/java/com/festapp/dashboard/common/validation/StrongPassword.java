// 커스텀 검증 애노테이션 - 강력한 비밀번호 검증
package com.festapp.dashboard.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * 강력한 비밀번호 검증 애노테이션
 *
 * 비밀번호 요구사항:
 * - 최소 8자
 * - 영문 대문자 최소 1개
 * - 영문 소문자 최소 1개
 * - 숫자 최소 1개
 * - 특수문자 최소 1개
 *
 * @author DashBoar Team
 * @version 1.0.0
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = StrongPasswordValidator.class)
@Documented
public @interface StrongPassword {
    String message() default "비밀번호는 영문 대문자, 소문자, 숫자, 특수문자를 포함하고 최소 8자 이상이어야 합니다";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

