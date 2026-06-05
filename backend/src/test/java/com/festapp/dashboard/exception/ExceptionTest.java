// 예외 클래스 테스트
package com.festapp.dashboard.exception;

import com.festapp.dashboard.common.exception.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("예외 클래스 테스트")
public class ExceptionTest {

    @Test
    @DisplayName("BusinessException 생성 - 에러 코드만 전달")
    void testBusinessExceptionWithErrorCode() {
        // Given & When
        BusinessException exception = new BusinessException(ErrorCode.INVALID_INPUT);

        // Then
        assertThat(exception).isNotNull();
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT);
        assertThat(exception.getErrorMessage()).isEqualTo("입력값이 유효하지 않습니다");
        assertThat(exception.getDetailMessage()).isNull();
        assertThat(exception.getErrorStatus()).isEqualTo(400);
    }

    @Test
    @DisplayName("BusinessException 생성 - 에러 코드 및 상세 메시지 전달")
    void testBusinessExceptionWithErrorCodeAndDetail() {
        // Given
        String detailMessage = "email field: 유효한 형식이 아닙니다";

        // When
        BusinessException exception = new BusinessException(ErrorCode.INVALID_INPUT, detailMessage);

        // Then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT);
        assertThat(exception.getDetailMessage()).isEqualTo(detailMessage);
        assertThat(exception.getMessage()).contains(detailMessage);
    }

    @Test
    @DisplayName("BusinessException 생성 - 원인 예외 포함")
    void testBusinessExceptionWithCause() {
        // Given
        Throwable cause = new IllegalArgumentException("원인");

        // When
        BusinessException exception = new BusinessException(
                ErrorCode.DATABASE_ERROR,
                "데이터베이스 연결 실패",
                cause
        );

        // Then
        assertThat(exception.getCause()).isEqualTo(cause);
        assertThat(exception.getDetailMessage()).isEqualTo("데이터베이스 연결 실패");
    }

    @Test
    @DisplayName("ResourceNotFoundException 생성")
    void testResourceNotFoundException() {
        // Given & When
        ResourceNotFoundException exception = new ResourceNotFoundException(
                ErrorCode.USER_NOT_FOUND,
                "userId: 123"
        );

        // Then
        assertThat(exception).isInstanceOf(BusinessException.class);
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
        assertThat(exception.getDetailMessage()).isEqualTo("userId: 123");
    }

    @Test
    @DisplayName("ValidationException 생성")
    void testValidationException() {
        // Given & When
        ValidationException exception = new ValidationException(
                ErrorCode.PASSWORD_MISMATCH,
                "새 비밀번호는 현재 비밀번호와 달라야 합니다"
        );

        // Then
        assertThat(exception).isInstanceOf(BusinessException.class);
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PASSWORD_MISMATCH);
        assertThat(exception.getDetailMessage()).contains("비밀번호");
    }

    @Test
    @DisplayName("ErrorCode 조회 - 에러 코드 정보")
    void testErrorCodeProperties() {
        // Given
        ErrorCode errorCode = ErrorCode.USER_NOT_FOUND;

        // When & Then
        assertThat(errorCode.getCode()).isEqualTo(4002);
        assertThat(errorCode.getHttpStatus()).isEqualTo(404);
        assertThat(errorCode.getHttpStatusEnum().value()).isEqualTo(404);
        assertThat(errorCode.getMessage()).isEqualTo("사용자를 찾을 수 없습니다");
    }

    @Test
    @DisplayName("ErrorCode - 다양한 종류의 에러 코드 확인")
    void testVariousErrorCodes() {
        // 인증 관련
        assertThat(ErrorCode.INVALID_CREDENTIALS.getCode()).isEqualTo(4001);
        assertThat(ErrorCode.AUTHENTICATION_REQUIRED.getCode()).isEqualTo(4007);

        // 검증 관련
        assertThat(ErrorCode.INVALID_INPUT.getCode()).isEqualTo(4100);
        assertThat(ErrorCode.WEAK_PASSWORD.getCode()).isEqualTo(4102);

        // 리소스 관련
        assertThat(ErrorCode.RESOURCE_NOT_FOUND.getCode()).isEqualTo(4200);
        assertThat(ErrorCode.WIDGET_NOT_FOUND.getCode()).isEqualTo(4201);

        // 비즈니스 로직
        assertThat(ErrorCode.INVALID_STATE_TRANSITION.getCode()).isEqualTo(4300);

        // 서버 에러
        assertThat(ErrorCode.INTERNAL_SERVER_ERROR.getCode()).isEqualTo(5000);
    }

    @Test
    @DisplayName("예외 메시지 체인 확인")
    void testExceptionMessageChain() {
        // Given
        Throwable cause = new RuntimeException("root cause");
        BusinessException exception = new BusinessException(
                ErrorCode.INVALID_INPUT,
                "validation failed",
                cause
        );

        // When & Then
        assertThat(exception).hasRootCause(cause);
        assertThat(exception.getMessage()).contains("입력값이 유효하지 않습니다");
        assertThat(exception.getMessage()).contains("validation failed");
    }
}

