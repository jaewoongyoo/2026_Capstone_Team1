// 본인 소유가 아닌 위젯에 접근할 때 발생하는 예외 - Dashboard Widget 계층
package com.festapp.dashboard.dashboard.widget.exception;

import com.festapp.dashboard.common.exception.BusinessException;
import com.festapp.dashboard.common.exception.ErrorCode;

/**
 * 위젯 접근 권한 부재시 발생하는 예외
 *
 * 다른 사용자의 위젯에 접근하려거나 권한이 없는 작업을 시도할 때 발생합니다.
 *
 * 예시:
 * - throw new WidgetAccessDeniedException(ErrorCode.WIDGET_ACCESS_DENIED);
 * - throw new WidgetAccessDeniedException(ErrorCode.WIDGET_ACCESS_DENIED, "widgetId: 123");
 *
 * @author DashBoar Team
 * @version 1.0.0
 */
public class WidgetAccessDeniedException extends BusinessException {

    /**
     * 에러 코드와 함께 예외 생성
     *
     * @param errorCode 접근 거부 관련 에러 코드
     */
    public WidgetAccessDeniedException(ErrorCode errorCode) {
        super(errorCode);
    }

    /**
     * 에러 코드와 상세 메시지와 함께 예외 생성
     *
     * @param errorCode 접근 거부 관련 에러 코드
     * @param detailMessage 상세 메시지 (예: 위젯 ID)
     */
    public WidgetAccessDeniedException(ErrorCode errorCode, String detailMessage) {
        super(errorCode, detailMessage);
    }

    /**
     * 에러 코드, 상세 메시지, 원인 예외와 함께 생성
     *
     * @param errorCode 접근 거부 관련 에러 코드
     * @param detailMessage 상세 메시지
     * @param cause 원인 예외
     */
    public WidgetAccessDeniedException(ErrorCode errorCode, String detailMessage, Throwable cause) {
        super(errorCode, detailMessage, cause);
    }

    /**
     * 문자열 메시지로만 예외 생성 (하위 호환성)
     *
     * @param message 오류 메시지
     * @deprecated ErrorCode를 사용하는 생성자를 권장합니다
     */
    @Deprecated
    public WidgetAccessDeniedException(String message) {
        super(ErrorCode.WIDGET_ACCESS_DENIED, message);
    }

    /**
     * 문자열 메시지와 원인 예외로 생성 (하위 호환성)
     *
     * @param message 오류 메시지
     * @param cause 원인 예외
     * @deprecated ErrorCode를 사용하는 생성자를 권장합니다
     */
    @Deprecated
    public WidgetAccessDeniedException(String message, Throwable cause) {
        super(ErrorCode.WIDGET_ACCESS_DENIED, message, cause);
    }
}

