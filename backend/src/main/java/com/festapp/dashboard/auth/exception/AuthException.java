// 인증 예외 - 인증 실패시 던지는 커스텀 예외
package com.festapp.dashboard.auth.exception;

import com.festapp.dashboard.common.exception.BusinessException;
import com.festapp.dashboard.common.exception.ErrorCode;

/**
 * 인증 관련 예외
 *
 * 로그인, 회원가입, 토큰 검증 등 인증 관련 작업에서 발생하는 예외입니다.
 *
 * 예시:
 * - throw new AuthException(ErrorCode.INVALID_CREDENTIALS);
 * - throw new AuthException(ErrorCode.EMAIL_ALREADY_EXISTS, "user@example.com");
 *
 * @author DashBoar Team
 * @version 1.0.0
 */
public class AuthException extends BusinessException {

    /**
     * 에러 코드와 함께 예외 생성
     *
     * @param errorCode 인증 관련 에러 코드
     */
    public AuthException(ErrorCode errorCode) {
        super(errorCode);
    }

    /**
     * 에러 코드와 상세 메시지와 함께 예외 생성
     *
     * @param errorCode 인증 관련 에러 코드
     * @param detailMessage 상세 메시지 (예: 중복된 이메일)
     */
    public AuthException(ErrorCode errorCode, String detailMessage) {
        super(errorCode, detailMessage);
    }

    /**
     * 에러 코드, 상세 메시지, 원인 예외와 함께 생성
     *
     * @param errorCode 인증 관련 에러 코드
     * @param detailMessage 상세 메시지
     * @param cause 원인 예외
     */
    public AuthException(ErrorCode errorCode, String detailMessage, Throwable cause) {
        super(errorCode, detailMessage, cause);
    }

    /**
     * 문자열 메시지로만 예외 생성 (하위 호환성)
     *
     * @param message 오류 메시지
     * @deprecated ErrorCode를 사용하는 생성자를 권장합니다
     */
    @Deprecated
    public AuthException(String message) {
        super(ErrorCode.INVALID_CREDENTIALS, message);
    }

    /**
     * 문자열 메시지와 원인 예외로 생성 (하위 호환성)
     *
     * @param message 오류 메시지
     * @param cause 원인 예외
     * @deprecated ErrorCode를 사용하는 생성자를 권장합니다
     */
    @Deprecated
    public AuthException(String message, Throwable cause) {
        super(ErrorCode.INVALID_CREDENTIALS, message, cause);
    }
}

