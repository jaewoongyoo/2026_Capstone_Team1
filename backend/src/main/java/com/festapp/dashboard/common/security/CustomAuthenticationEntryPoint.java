package com.festapp.dashboard.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.festapp.dashboard.common.dto.ApiResponse;
import com.festapp.dashboard.common.exception.ErrorCode;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * 커스텀 인증 진입점
 *
 * 인증되지 않은 요청에 대해 401 Unauthorized 응답을 반환합니다.
 * Spring Security의 기본 403 응답 대신 401을 반환하도록 설정합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {
        log.warn("Authentication failed - {}: {}", request.getRequestURI(), authException.getMessage());

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ApiResponse<?> apiResponse = ApiResponse.error(
                "인증이 필요합니다",
                ErrorCode.AUTHENTICATION_REQUIRED.getCode(),
                "유효한 JWT 토큰을 제공해주세요",
                HttpServletResponse.SC_UNAUTHORIZED
        );
        apiResponse.setTimestamp(LocalDateTime.now());
        apiResponse.setPath(request.getRequestURI());

        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }
}

