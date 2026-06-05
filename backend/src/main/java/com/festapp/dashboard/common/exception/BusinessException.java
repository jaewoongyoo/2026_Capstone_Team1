// 기본 비즈니스 예외 클래스 - 모든 커스텀 예외의 기본 클래스
package com.festapp.dashboard.common.exception;

/**
 * 비즈니스 로직 예외의 기본 클래스
 *
 * 모든 커스텀 예외는 이 클래스를 상속받아야 합니다.
 * 에러 코드와 상세 메시지를 함께 제공합니다.
 *
 * @author DashBoar Team
 * @version 1.0.0
 */
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;
    private final String detailMessage;

    /**
     * 에러 코드와 함께 예외 생성
     *
     * @param errorCode 에러 코드 (API 클라이언트가 처리할 수 있는 코드)
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.detailMessage = null;
    }

    /**
     * 에러 코드와 상세 메시지와 함께 예외 생성
     *
     * @param errorCode 에러 코드
     * @param detailMessage 상세 메시지 (개발자/사용자에게 더 구체적인 정보 제공)
     */
    public BusinessException(ErrorCode errorCode, String detailMessage) {
        super(errorCode.getMessage() + (detailMessage != null ? " - " + detailMessage : ""));
        this.errorCode = errorCode;
        this.detailMessage = detailMessage;
    }

    /**
     * 에러 코드, 상세 메시지, 원인 예외와 함께 생성
     *
     * @param errorCode 에러 코드
     * @param detailMessage 상세 메시지
     * @param cause 원인 예외
     */
    public BusinessException(ErrorCode errorCode, String detailMessage, Throwable cause) {
        super(errorCode.getMessage() + (detailMessage != null ? " - " + detailMessage : ""), cause);
        this.errorCode = errorCode;
        this.detailMessage = detailMessage;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public String getDetailMessage() {
        return detailMessage;
    }

    public String getErrorMessage() {
        return errorCode.getMessage();
    }

    public int getErrorStatus() {
        return errorCode.getHttpStatus();
    }
}

