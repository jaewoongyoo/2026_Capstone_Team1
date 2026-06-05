// 위젯을 찾을 수 없을 때 발생하는 예외 - Dashboard Widget 계층
package com.festapp.dashboard.dashboard.widget.exception;

import com.festapp.dashboard.common.exception.BusinessException;
import com.festapp.dashboard.common.exception.ErrorCode;

/**
 * 위젯을 찾을 수 없을 때 발생하는 예외
 *
 * 특정 ID의 위젯이 존재하지 않거나 삭제되었을 때 발생합니다.
 *
 * 예시:
 * - throw new WidgetNotFoundException(ErrorCode.WIDGET_NOT_FOUND);
 * - throw new WidgetNotFoundException(ErrorCode.WIDGET_NOT_FOUND, "widgetId: 123");
 *
 * @author DashBoar Team
 * @version 1.0.0
 */
public class WidgetNotFoundException extends BusinessException {

    /**
     * 에러 코드와 함께 예외 생성
     *
     * @param errorCode 위젯 관련 에러 코드
     */
    public WidgetNotFoundException(ErrorCode errorCode) {
        super(errorCode);
    }

    /**
     * 에러 코드와 상세 메시지와 함께 예외 생성
     *
     * @param errorCode 위젯 관련 에러 코드
     * @param detailMessage 상세 메시지 (예: 위젯 ID)
     */
    public WidgetNotFoundException(ErrorCode errorCode, String detailMessage) {
        super(errorCode, detailMessage);
    }

    /**
     * 에러 코드, 상세 메시지, 원인 예외와 함께 생성
     *
     * @param errorCode 위젯 관련 에러 코드
     * @param detailMessage 상세 메시지
     * @param cause 원인 예외
     */
    public WidgetNotFoundException(ErrorCode errorCode, String detailMessage, Throwable cause) {
        super(errorCode, detailMessage, cause);
    }

    /**
     * 문자열 메시지로만 예외 생성 (하위 호환성)
     *
     * @param message 오류 메시지
     * @deprecated ErrorCode를 사용하는 생성자를 권장합니다
     */
    @Deprecated
    public WidgetNotFoundException(String message) {
        super(ErrorCode.WIDGET_NOT_FOUND, message);
    }

    /**
     * 문자열 메시지와 원인 예외로 생성 (하위 호환성)
     *
     * @param message 오류 메시지
     * @param cause 원인 예외
     * @deprecated ErrorCode를 사용하는 생성자를 권장합니다
     */
    @Deprecated
    public WidgetNotFoundException(String message, Throwable cause) {
        super(ErrorCode.WIDGET_NOT_FOUND, message, cause);
    }
}

