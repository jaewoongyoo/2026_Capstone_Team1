// 보안 컨텍스트 헬퍼 - 현재 인증된 사용자 정보 조회 유틸리티
package com.festapp.dashboard.common.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * 보안 컨텍스트에서 현재 인증된 사용자 정보를 조회하는 유틸리티 클래스
 */
@Slf4j
@Component
public class SecurityContextHelper {

    /**
     * 현재 인증된 사용자의 userId를 조회
     * @return 사용자 ID
     * @throws IllegalStateException 인증되지 않은 경우
     */
    public static Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (isAnonymous(authentication)) {
            log.warn("사용자가 인증되지 않았습니다");
            throw new IllegalStateException("사용자가 인증되지 않았습니다");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserDetails) {
            Long userId = ((CustomUserDetails) principal).getUserId();
            log.debug("현재 userId 추출: {}", userId);
            return userId;
        }

        log.error("인증에서 사용자 ID를 추출할 수 없습니다. Principal 타입: {}", principal.getClass().getName());
        throw new IllegalStateException("인증에서 사용자 ID를 추출할 수 없습니다");
    }

    /**
     * 현재 인증된 사용자의 username을 조회
     * @return 사용자명
     * @throws IllegalStateException 인증되지 않은 경우
     */
    public static String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (isAnonymous(authentication)) {
            log.warn("사용자가 인증되지 않았습니다");
            throw new IllegalStateException("사용자가 인증되지 않았습니다");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserDetails) {
            String username = ((CustomUserDetails) principal).getUsername();
            log.debug("현재 username 추출: {}", username);
            return username;
        }

        log.error("인증에서 사용자명을 추출할 수 없습니다. Principal 타입: {}", principal.getClass().getName());
        throw new IllegalStateException("인증에서 사용자명을 추출할 수 없습니다");
    }

    /**
     * 현재 인증된 사용자의 role을 조회
     * @return 역할
     * @throws IllegalStateException 인증되지 않은 경우
     */
    public static String getCurrentRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (isAnonymous(authentication)) {
            log.warn("사용자가 인증되지 않았습니다");
            throw new IllegalStateException("사용자가 인증되지 않았습니다");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserDetails) {
            String role = ((CustomUserDetails) principal).getRole();
            log.debug("현재 role 추출: {}", role);
            return role;
        }

        log.error("인증에서 역할을 추출할 수 없습니다. Principal 타입: {}", principal.getClass().getName());
        throw new IllegalStateException("인증에서 역할을 추출할 수 없습니다");
    }

    /**
     * 현재 인증된 사용자의 CustomUserDetails를 조회
     * @return CustomUserDetails 객체
     * @throws IllegalStateException 인증되지 않은 경우
     */
    public static CustomUserDetails getCurrentUserDetails() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (isAnonymous(authentication)) {
            log.warn("사용자가 인증되지 않았습니다");
            throw new IllegalStateException("사용자가 인증되지 않았습니다");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserDetails) {
            log.debug("현재 사용자 정보 추출 완료");
            return (CustomUserDetails) principal;
        }

        log.error("인증에서 사용자 정보를 추출할 수 없습니다. Principal 타입: {}", principal.getClass().getName());
        throw new IllegalStateException("인증에서 사용자 정보를 추출할 수 없습니다");
    }

    /**
     * 사용자가 인증되었는지 확인
     * @return 인증 여부
     */
    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return !isAnonymous(authentication);
    }

    private static boolean isAnonymous(Authentication authentication) {
        return authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken;
    }
}
