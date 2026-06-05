// 검증 예외 - 입력값 검증 실패시 던지는 예외
package com.festapp.dashboard.common.exception;

/**
 * 데이터 검증 실패시 발생하는 예외
 *
 * 비밀번호 불일치, 잘못된 입력값, 상태 전환 불가 등 비즈니스 로직 검증 실패시 발생합니다.
 *
 * 예시:
 * - throw new ValidationException(ErrorCode.PASSWORD_MISMATCH);
 * - throw new ValidationException(ErrorCode.INVALID_INPUT, "포맷: YYYY-MM-DD");
 *
 * @author DashBoar Team
 * @version 1.0.0
 */
public class ValidationException extends BusinessException {

    /**
     * 에러 코드와 함께 예외 생성
     *
     * @param errorCode 검증 관련 에러 코드
     */
    public ValidationException(ErrorCode errorCode) {
        super(errorCode);
    }

    /**
     * 에러 코드와 상세 메시지와 함께 예외 생성
     *
     * @param errorCode 검증 관련 에러 코드
     * @param detailMessage 상세 메시지 (예: 올바른 포맷)
     */
    public ValidationException(ErrorCode errorCode, String detailMessage) {
        super(errorCode, detailMessage);
    }

    /**
     * 에러 코드, 상세 메시지, 원인 예외와 함께 생성
     *
     * @param errorCode 검증 관련 에러 코드
     * @param detailMessage 상세 메시지
     * @param cause 원인 예외
     */
    public ValidationException(ErrorCode errorCode, String detailMessage, Throwable cause) {
        super(errorCode, detailMessage, cause);
    }

    /**
     * 문자열 메시지로만 예외 생성 (하위 호환성)
     *
     * @param message 오류 메시지
     * @deprecated ErrorCode를 사용하는 생성자를 권장합니다
     */
    @Deprecated
    public ValidationException(String message) {
        super(ErrorCode.INVALID_INPUT, message);
    }

    /**
     * 문자열 메시지와 원인 예외로 생성 (하위 호환성)
     *
     * @param message 오류 메시지
     * @param cause 원인 예외
     * @deprecated ErrorCode를 사용하는 생성자를 권장합니다
     */
    @Deprecated
    public ValidationException(String message, Throwable cause) {
        super(ErrorCode.INVALID_INPUT, message, cause);
    }
}

