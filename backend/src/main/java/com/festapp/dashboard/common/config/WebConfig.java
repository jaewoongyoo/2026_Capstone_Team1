// 웹 설정 - 인터셉터 등록
package com.festapp.dashboard.common.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 웹 설정
 *
 * 인터셉터, 포매터, 변환기 등을 등록합니다.
 *
 * @author DashBoar Team
 * @version 1.0.0
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final RequestLoggingInterceptor requestLoggingInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(requestLoggingInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                    "/swagger-ui.html",
                    "/swagger-ui/**",
                    "/v3/api-docs",
                    "/v3/api-docs/**",
                    "/api-docs",
                    "/api-docs/**",
                    "/health",
                    "/health/**",
                    "/api/public/health",
                    "/api/public/health/**"
                );
    }
}

