// 에러 코드 정의 - 클라이언트가 처리할 수 있는 표준화된 에러 코드
package com.festapp.dashboard.common.exception;

import org.springframework.http.HttpStatus;

/**
 * API 에러 코드 정의
 *
 * 프론트엔드 개발자가 쉽게 처리할 수 있도록 명확한 에러 코드를 정의합니다.
 * 각 에러는 에러 코드, HTTP 상태 코드, 메시지를 포함합니다.
 *
 * @author DashBoar Team
 * @version 1.0.0
 */
public enum ErrorCode {
    // 인증 관련 에러 (4000-4099)
    INVALID_CREDENTIALS(4001, HttpStatus.UNAUTHORIZED, "잘못된 username 또는 password입니다"),
    USER_NOT_FOUND(4002, HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다"),
    USER_NOT_ACTIVE(4003, HttpStatus.UNAUTHORIZED, "비활성화된 계정입니다"),
    EMAIL_ALREADY_EXISTS(4004, HttpStatus.BAD_REQUEST, "이미 존재하는 email입니다"),
    USERNAME_ALREADY_EXISTS(4005, HttpStatus.BAD_REQUEST, "이미 존재하는 username입니다"),
    INVALID_TOKEN(4006, HttpStatus.UNAUTHORIZED, "유효하지 않거나 만료된 토큰입니다"),
    AUTHENTICATION_REQUIRED(4007, HttpStatus.UNAUTHORIZED, "인증이 필요합니다"),
    INSUFFICIENT_PERMISSION(4008, HttpStatus.FORBIDDEN, "권한이 없습니다"),

    // 검증 관련 에러 (4100-4199)
    INVALID_INPUT(4100, HttpStatus.BAD_REQUEST, "입력값이 유효하지 않습니다"),
    INVALID_EMAIL_FORMAT(4101, HttpStatus.BAD_REQUEST, "유효한 이메일 형식이 아닙니다"),
    WEAK_PASSWORD(4102, HttpStatus.BAD_REQUEST, "비밀번호가 약합니다. 영문, 숫자, 특수문자를 포함해야 합니다"),
    PASSWORD_MISMATCH(4103, HttpStatus.BAD_REQUEST, "비밀번호가 일치하지 않습니다"),
    PASSWORD_SAME_AS_CURRENT(4104, HttpStatus.BAD_REQUEST, "새 비밀번호는 현재 비밀번호와 달라야 합니다"),
    INVALID_FIELD_VALUE(4105, HttpStatus.BAD_REQUEST, "필드값이 유효하지 않습니다"),

    // 리소스 관련 에러 (4200-4299)
    RESOURCE_NOT_FOUND(4200, HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다"),
    WIDGET_NOT_FOUND(4201, HttpStatus.NOT_FOUND, "위젯을 찾을 수 없습니다"),
    EQUIPMENT_NOT_FOUND(4202, HttpStatus.NOT_FOUND, "장비를 찾을 수 없습니다"),
    WIDGET_ACCESS_DENIED(4203, HttpStatus.FORBIDDEN, "이 위젯에 접근할 권한이 없습니다"),
    RESOURCE_ALREADY_EXISTS(4204, HttpStatus.BAD_REQUEST, "해당 리소스가 이미 존재합니다"),
    DASHBOARD_NOT_FOUND(4205, HttpStatus.NOT_FOUND, "대시보드를 찾을 수 없습니다"),
    SENSOR_NOT_FOUND(4206, HttpStatus.NOT_FOUND, "센서를 찾을 수 없습니다"),

    // 비즈니스 로직 에러 (4300-4399)
    INVALID_STATE_TRANSITION(4300, HttpStatus.BAD_REQUEST, "유효하지 않은 상태 전환입니다"),
    DUPLICATE_ACTIVATION(4301, HttpStatus.BAD_REQUEST, "이미 활성화된 사용자입니다"),
    DUPLICATE_DEACTIVATION(4302, HttpStatus.BAD_REQUEST, "이미 비활성화된 사용자입니다"),
    INVALID_ROLE_CHANGE(4303, HttpStatus.BAD_REQUEST, "유효하지 않은 역할 변경입니다"),

    // 서버 에러 (5000-5099)
    INTERNAL_SERVER_ERROR(5000, HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다"),
    DATABASE_ERROR(5001, HttpStatus.INTERNAL_SERVER_ERROR, "데이터베이스 오류가 발생했습니다"),
    EXTERNAL_SERVICE_ERROR(5002, HttpStatus.INTERNAL_SERVER_ERROR, "외부 서비스 오류가 발생했습니다");

    private final int code;
    private final HttpStatus httpStatus;
    private final String message;

    ErrorCode(int code, HttpStatus httpStatus, String message) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public HttpStatus getHttpStatusEnum() {
        return httpStatus;
    }

    public int getHttpStatus() {
        return httpStatus.value();
    }

    public String getMessage() {
        return message;
    }
}

