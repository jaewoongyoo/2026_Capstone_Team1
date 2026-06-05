// API 요청/응답 로깅 인터셉터
package com.festapp.dashboard.common.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.util.UUID;

/**
 * API 요청/응답 로깅 인터셉터
 *
 * 모든 HTTP 요청/응답을 로깅하고 Correlation ID를 할당합니다.
 *
 * @author DashBoar Team
 * @version 1.0.0
 */
@Slf4j
@Component
public class RequestLoggingInterceptor implements HandlerInterceptor {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    public static final String CORRELATION_ID_ATTRIBUTE = "correlationId";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // Correlation ID 생성 또는 기존값 사용
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isEmpty()) {
            correlationId = UUID.randomUUID().toString();
        }

        // 요청 정보 저장
        request.setAttribute(CORRELATION_ID_ATTRIBUTE, correlationId);
        response.setHeader(CORRELATION_ID_HEADER, correlationId);

        // 요청 로깅
        long startTime = System.currentTimeMillis();
        request.setAttribute("startTime", startTime);

        log.info("[{}] {} {} - Client IP: {}",
                correlationId,
                request.getMethod(),
                request.getRequestURI(),
                getClientIp(request)
        );

        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, 
                          Object handler, ModelAndView modelAndView) {
        // 응답 후 처리 (필요시)
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, 
                               Object handler, Exception ex) {
        String correlationId = (String) request.getAttribute(CORRELATION_ID_ATTRIBUTE);
        Long startTime = (Long) request.getAttribute("startTime");
        long duration = System.currentTimeMillis() - startTime;

        if (ex != null) {
            log.error("[{}] {} {} - Status: {} - Duration: {}ms - Error: {}",
                    correlationId,
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    duration,
                    ex.getMessage()
            );
        } else {
            log.info("[{}] {} {} - Status: {} - Duration: {}ms",
                    correlationId,
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    duration
            );
        }
    }

    /**
     * 클라이언트 IP 주소 추출
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }
}

