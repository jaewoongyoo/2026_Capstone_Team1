// 전역 예외 처리기 - 모든 예외를 일관되게 처리
package com.festapp.dashboard.common.exception;

import com.festapp.dashboard.common.dto.ApiResponse;
import com.festapp.dashboard.auth.exception.AuthException;
import com.festapp.dashboard.dashboard.widget.exception.WidgetNotFoundException;
import com.festapp.dashboard.dashboard.widget.exception.WidgetAccessDeniedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;

/**
 * 전역 예외 처리기
 * 
 * 애플리케이션 전체에서 발생하는 모든 예외를 일관되게 처리합니다.
 * 예외를 표준 ApiResponse 형식으로 변환하여 클라이언트에 반환합니다.
 * 
 * 프론트엔드 개발자는 errorCode를 통해 프로그래밍방식으로 오류를 처리할 수 있습니다.
 * 
 * @author DashBoar Team
 * @version 1.0.0
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 비즈니스 예외 처리
     * 
     * 모든 커스텀 BusinessException을 처리합니다.
     * 에러 코드와 상세 메시지를 포함한 응답을 반환합니다.
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<?>> handleBusinessException(BusinessException ex, WebRequest request) {
        log.warn("Business Exception - ErrorCode: {}, Message: {}, Detail: {}",
                ex.getErrorCode(),
                ex.getErrorMessage(),
                ex.getDetailMessage());

        ApiResponse<?> response = ApiResponse.error(
                ex.getErrorMessage(),
                ex.getErrorCode().getCode(),
                ex.getDetailMessage(),
                ex.getErrorStatus()
        );
        response.setPath(extractPath(request));
        response.setTimestamp(LocalDateTime.now());

        return new ResponseEntity<>(response, ex.getErrorCode().getHttpStatusEnum());
    }

    /**
     * 인증 예외 처리
     * 
     * AuthException은 BusinessException을 상속받으므로,
     * 위 핸들러에서 처리됩니다.
     * 이 메서드는 명시적 처리가 필요한 경우를 위해 남겨둡니다.
     */
    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ApiResponse<?>> handleAuthException(AuthException ex, WebRequest request) {
        log.warn("Auth Exception - ErrorCode: {}, Message: {}", ex.getErrorCode(), ex.getErrorMessage());
        
        ApiResponse<?> response = ApiResponse.error(
                ex.getErrorMessage(),
                ex.getErrorCode().getCode(),
                ex.getDetailMessage(),
                ex.getErrorStatus()
        );
        response.setPath(extractPath(request));
        
        return new ResponseEntity<>(response, ex.getErrorCode().getHttpStatusEnum());
    }

    /**
     * 리소스 없음 예외 처리
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<?>> handleResourceNotFoundException(ResourceNotFoundException ex, WebRequest request) {
        log.warn("Resource Not Found - ErrorCode: {}, Message: {}", ex.getErrorCode(), ex.getErrorMessage());
        
        ApiResponse<?> response = ApiResponse.error(
                ex.getErrorMessage(),
                ex.getErrorCode().getCode(),
                ex.getDetailMessage(),
                ex.getErrorStatus()
        );
        response.setPath(extractPath(request));
        
        return new ResponseEntity<>(response, ex.getErrorCode().getHttpStatusEnum());
    }

    /**
     * 검증 예외 처리
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiResponse<?>> handleValidationException(ValidationException ex, WebRequest request) {
        log.warn("Validation Exception - ErrorCode: {}, Message: {}", ex.getErrorCode(), ex.getErrorMessage());
        
        ApiResponse<?> response = ApiResponse.error(
                ex.getErrorMessage(),
                ex.getErrorCode().getCode(),
                ex.getDetailMessage(),
                ex.getErrorStatus()
        );
        response.setPath(extractPath(request));
        
        return new ResponseEntity<>(response, ex.getErrorCode().getHttpStatusEnum());
    }

    /**
     * 위젯 없음 예외 처리
     */
    @ExceptionHandler(WidgetNotFoundException.class)
    public ResponseEntity<ApiResponse<?>> handleWidgetNotFoundException(WidgetNotFoundException ex, WebRequest request) {
        log.warn("Widget Not Found - ErrorCode: {}, Message: {}", ex.getErrorCode(), ex.getErrorMessage());
        
        ApiResponse<?> response = ApiResponse.error(
                ex.getErrorMessage(),
                ex.getErrorCode().getCode(),
                ex.getDetailMessage(),
                HttpStatus.NOT_FOUND.value()
        );
        response.setPath(extractPath(request));
        
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    /**
     * 위젯 접근 거부 예외 처리
     */
    @ExceptionHandler(WidgetAccessDeniedException.class)
    public ResponseEntity<ApiResponse<?>> handleWidgetAccessDeniedException(WidgetAccessDeniedException ex, WebRequest request) {
        log.warn("Widget Access Denied - ErrorCode: {}, Message: {}", ex.getErrorCode(), ex.getErrorMessage());
        
        ApiResponse<?> response = ApiResponse.error(
                ex.getErrorMessage(),
                ex.getErrorCode().getCode(),
                ex.getDetailMessage(),
                HttpStatus.FORBIDDEN.value()
        );
        response.setPath(extractPath(request));
        
        return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
    }

    /**
     * 입력값 검증 실패 처리 (스프링 자체 검증)
     * 
     * @Valid 애노테이션으로 인한 검증 실패를 처리합니다.
     * 첫 번째 오류를 반환하며, 상세하게 어느 필드가 오류인지 명시합니다.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<?>> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex, WebRequest request) {
        log.warn("Method Argument Not Valid");
        
        String fieldName = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst()
                .orElse("입력값이 유효하지 않습니다");

        ApiResponse<?> response = ApiResponse.error(
                "입력값 검증 실패",
                ErrorCode.INVALID_INPUT.getCode(),
                fieldName,
                HttpStatus.BAD_REQUEST.value()
        );
        response.setPath(extractPath(request));
        
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * 핸들러 메서드 검증 예외 처리 (Spring Boot 3.3+)
     *
     * @RequestBody의 @Valid 검증 실패를 처리합니다.
     */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiResponse<?>> handleHandlerMethodValidationException(HandlerMethodValidationException ex, WebRequest request) {
        log.warn("Handler Method Validation Exception");

        String fieldName = ex.getAllErrors().stream()
                .map(error -> error.getDefaultMessage())
                .findFirst()
                .orElse("입력값이 유효하지 않습니다");

        ApiResponse<?> response = ApiResponse.error(
                "입력값 검증 실패",
                ErrorCode.INVALID_INPUT.getCode(),
                fieldName,
                HttpStatus.BAD_REQUEST.value()
        );
        response.setPath(extractPath(request));

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * JSON 본문 파싱 실패 처리
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<?>> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex, WebRequest request) {
        log.warn("HTTP Message Not Readable: {}", ex.getMessage());

        ApiResponse<?> response = ApiResponse.error(
                "요청 본문을 읽을 수 없습니다",
                ErrorCode.INVALID_INPUT.getCode(),
                ex.getMostSpecificCause().getMessage(),
                HttpStatus.BAD_REQUEST.value()
        );
        response.setPath(extractPath(request));

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * 쿼리 파라미터 및 경로 변수 타입 변환 실패 처리
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<?>> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException ex, WebRequest request) {
        log.warn("Method Argument Type Mismatch - name: {}, value: {}", ex.getName(), ex.getValue());

        ApiResponse<?> response = ApiResponse.error(
                "요청 파라미터 형식이 올바르지 않습니다",
                ErrorCode.INVALID_INPUT.getCode(),
                ex.getName() + ": " + ex.getValue(),
                HttpStatus.BAD_REQUEST.value()
        );
        response.setPath(extractPath(request));

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * 잘못된 enum 값 등 일반 인자 오류 처리
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<?>> handleIllegalArgumentException(IllegalArgumentException ex, WebRequest request) {
        log.warn("Illegal Argument Exception: {}", ex.getMessage());

        ApiResponse<?> response = ApiResponse.error(
                "요청값이 올바르지 않습니다",
                ErrorCode.INVALID_INPUT.getCode(),
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value()
        );
        response.setPath(extractPath(request));

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * 존재하지 않는 API 경로 처리
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<?>> handleNoResourceFoundException(NoResourceFoundException ex, WebRequest request) {
        log.warn("No Resource Found: {}", ex.getMessage());

        ApiResponse<?> response = ApiResponse.error(
                ErrorCode.RESOURCE_NOT_FOUND.getMessage(),
                ErrorCode.RESOURCE_NOT_FOUND.getCode(),
                ex.getMessage(),
                HttpStatus.NOT_FOUND.value()
        );
        response.setPath(extractPath(request));

        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    /**
     * 지원하지 않는 HTTP 메서드 처리
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<?>> handleHttpRequestMethodNotSupportedException(
            HttpRequestMethodNotSupportedException ex,
            WebRequest request
    ) {
        log.warn("HTTP Method Not Supported: {}", ex.getMessage());

        ApiResponse<?> response = ApiResponse.error(
                "지원하지 않는 HTTP 메서드입니다",
                ErrorCode.INVALID_INPUT.getCode(),
                ex.getMessage(),
                HttpStatus.METHOD_NOT_ALLOWED.value()
        );
        response.setPath(extractPath(request));

        return new ResponseEntity<>(response, HttpStatus.METHOD_NOT_ALLOWED);
    }

    /**
     * 지원하지 않는 Content-Type 처리
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<?>> handleHttpMediaTypeNotSupportedException(
            HttpMediaTypeNotSupportedException ex,
            WebRequest request
    ) {
        log.warn("HTTP Media Type Not Supported: {}", ex.getMessage());

        ApiResponse<?> response = ApiResponse.error(
                "지원하지 않는 Content-Type입니다",
                ErrorCode.INVALID_INPUT.getCode(),
                ex.getMessage(),
                HttpStatus.UNSUPPORTED_MEDIA_TYPE.value()
        );
        response.setPath(extractPath(request));

        return new ResponseEntity<>(response, HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }

    /**
     * DB 제약조건 위반 처리
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<?>> handleDataIntegrityViolationException(
            DataIntegrityViolationException ex,
            WebRequest request
    ) {
        log.warn("Data Integrity Violation: {}", ex.getMostSpecificCause().getMessage());

        ApiResponse<?> response = ApiResponse.error(
                ErrorCode.RESOURCE_ALREADY_EXISTS.getMessage(),
                ErrorCode.RESOURCE_ALREADY_EXISTS.getCode(),
                ex.getMostSpecificCause().getMessage(),
                HttpStatus.CONFLICT.value()
        );
        response.setPath(extractPath(request));

        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }

    /**
     * 인증 자격증명 없음 처리
     */
    @ExceptionHandler(AuthenticationCredentialsNotFoundException.class)
    public ResponseEntity<ApiResponse<?>> handleAuthenticationCredentialsNotFoundException(AuthenticationCredentialsNotFoundException ex, WebRequest request) {
        log.warn("Authentication Credentials Not Found");
        
        ApiResponse<?> response = ApiResponse.error(
                "인증이 필요합니다",
                ErrorCode.AUTHENTICATION_REQUIRED.getCode(),
                null,
                HttpStatus.UNAUTHORIZED.value()
        );
        response.setPath(extractPath(request));
        
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }

    /**
     * 인증 거부 예외 처리 (@PreAuthorize)
     */
    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ApiResponse<?>> handleAuthorizationDeniedException(AuthorizationDeniedException ex, WebRequest request) {
        log.warn("Authorization Denied");
        
        ApiResponse<?> response = ApiResponse.error(
                "접근 권한이 없습니다",
                ErrorCode.INSUFFICIENT_PERMISSION.getCode(),
                null,
                HttpStatus.FORBIDDEN.value()
        );
        response.setPath(extractPath(request));
        
        return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
    }

    /**
     * IllegalStateException 처리
     * 
     * 인증되지 않은 상태에서 작업을 시도할 때 발생합니다.
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<?>> handleIllegalStateException(IllegalStateException ex, WebRequest request) {
        log.warn("Illegal State Exception: {}", ex.getMessage());
        
        // 인증 관련 메시지인지 확인
        if (ex.getMessage() != null && ex.getMessage().contains("인증")) {
            ApiResponse<?> response = ApiResponse.error(
                    ex.getMessage(),
                    ErrorCode.AUTHENTICATION_REQUIRED.getCode(),
                    null,
                    HttpStatus.UNAUTHORIZED.value()
            );
            response.setPath(extractPath(request));
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }
        
        ApiResponse<?> response = ApiResponse.error(
                ex.getMessage() != null ? ex.getMessage() : "잘못된 상태입니다",
                ErrorCode.INVALID_STATE_TRANSITION.getCode(),
                null,
                HttpStatus.BAD_REQUEST.value()
        );
        response.setPath(extractPath(request));
        
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * 모든 예기치 않은 예외 처리
     * 
     * 위에서 처리되지 않은 모든 예외를 처리합니다.
     * 스택트레이스를 로그에 기록합니다.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleException(Exception ex, WebRequest request) {
        log.error("Unexpected Exception", ex);
        
        ApiResponse<?> response = ApiResponse.error(
                "서버 오류가 발생했습니다",
                ErrorCode.INTERNAL_SERVER_ERROR.getCode(),
                ex.getMessage(),
                HttpStatus.INTERNAL_SERVER_ERROR.value()
        );
        response.setPath(extractPath(request));
        
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * 요청 경로 추출
     * 
     * @param request WebRequest 객체
     * @return 요청 경로
     */
    private String extractPath(WebRequest request) {
        String description = request.getDescription(false);
        // "uri=/api/users/1" 형식에서 경로만 추출
        return description.startsWith("uri=") ? description.substring(4) : description;
    }
}
