// 리소스 없음 예외 - 요청한 리소스가 존재하지 않을 때 던지는 예외
package com.festapp.dashboard.common.exception;

/**
 * 리소스를 찾을 수 없을 때 발생하는 예외
 * 
 * 사용자, 위젯, 장비 등 특정 리소스를 조회하려 할 때 존재하지 않으면 발생합니다.
 * 
 * 예시:
 * - throw new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND);
 * - throw new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "ID: 123");
 * 
 * @author DashBoar Team
 * @version 1.0.0
 */
public class ResourceNotFoundException extends BusinessException {
    
    /**
     * 에러 코드와 함께 예외 생성
     * 
     * @param errorCode 리소스 관련 에러 코드
     */
    public ResourceNotFoundException(ErrorCode errorCode) {
        super(errorCode);
    }

    /**
     * 에러 코드와 상세 메시지와 함께 예외 생성
     * 
     * @param errorCode 리소스 관련 에러 코드
     * @param detailMessage 상세 메시지 (예: 리소스 ID)
     */
    public ResourceNotFoundException(ErrorCode errorCode, String detailMessage) {
        super(errorCode, detailMessage);
    }

    /**
     * 에러 코드, 상세 메시지, 원인 예외와 함께 생성
     * 
     * @param errorCode 리소스 관련 에러 코드
     * @param detailMessage 상세 메시지
     * @param cause 원인 예외
     */
    public ResourceNotFoundException(ErrorCode errorCode, String detailMessage, Throwable cause) {
        super(errorCode, detailMessage, cause);
    }

    /**
     * 문자열 메시지로만 예외 생성 (하위 호환성)
     * 
     * @param message 오류 메시지
     * @deprecated ErrorCode를 사용하는 생성자를 권장합니다
     */
    @Deprecated
    public ResourceNotFoundException(String message) {
        super(ErrorCode.RESOURCE_NOT_FOUND, message);
    }

    /**
     * 문자열 메시지와 원인 예외로 생성 (하위 호환성)
     * 
     * @param message 오류 메시지
     * @param cause 원인 예외
     * @deprecated ErrorCode를 사용하는 생성자를 권장합니다
     */
    @Deprecated
    public ResourceNotFoundException(String message, Throwable cause) {
        super(ErrorCode.RESOURCE_NOT_FOUND, message, cause);
    }
}

