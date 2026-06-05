// JWT 인증 필터 - 요청에서 JWT 토큰 검증하고 보안 컨텍스트 설정
package com.festapp.dashboard.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final int BEARER_PREFIX_LENGTH = 7;
    private static final String ROLE_PREFIX = "ROLE_";

    private final JwtProvider jwtProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String jwt = getJwtFromRequest(request);

            if (StringUtils.hasText(jwt) && jwtProvider.validateToken(jwt)) {
                Long userId = jwtProvider.getUserIdFromJWT(jwt);
                String username = jwtProvider.getUsernameFromJWT(jwt);
                String role = jwtProvider.getRoleFromJWT(jwt);

                SimpleGrantedAuthority authority = new SimpleGrantedAuthority(ROLE_PREFIX + role);
                CustomUserDetails userDetails = CustomUserDetails.builder()
                        .userId(userId)
                        .username(username)
                        .role(role)
                        .isActive(true)
                        .build();

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, Collections.singletonList(authority));

                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("인증 완료 - {} {} (사용자: {}, userId: {})", request.getMethod(), request.getRequestURI(), username, userId);
            } else {
                log.debug("요청에서 유효한 JWT 토큰을 찾을 수 없습니다");
            }
        } catch (Exception e) {
            log.error("보안 컨텍스트에 사용자 인증을 설정할 수 없습니다: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 요청 헤더에서 JWT 토큰 추출
     */
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX_LENGTH);
        }
        return null;
    }
}

