// 인증 서비스 - 로그인, 회원가입, 토큰 갱신 등 비즈니스 로직 처리
package com.festapp.dashboard.auth.service;

import com.festapp.dashboard.auth.dto.*;
import com.festapp.dashboard.auth.entity.RefreshToken;
import com.festapp.dashboard.auth.exception.AuthException;
import com.festapp.dashboard.auth.repository.RefreshTokenRepository;
import com.festapp.dashboard.dashboard.service.DashboardProvisioningService;
import com.festapp.dashboard.user.dto.UserInfoResponse;
import com.festapp.dashboard.user.entity.User;
import com.festapp.dashboard.user.repository.UserRepository;
import com.festapp.dashboard.common.exception.ResourceNotFoundException;
import com.festapp.dashboard.common.exception.ValidationException;
import com.festapp.dashboard.common.exception.ErrorCode;
import com.festapp.dashboard.common.security.CustomUserDetails;
import com.festapp.dashboard.common.security.JwtProvider;
import com.festapp.dashboard.common.security.SecurityContextHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final long REFRESH_TOKEN_EXPIRATION_MS = 604800000L; // 7 days
    private static final String BEARER_TOKEN_TYPE = "Bearer";

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final AuthenticationManager authenticationManager;
    private final DashboardProvisioningService dashboardProvisioningService;

    /**
     * 공통 메서드: userId로 User 조회
     */
    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User not found with userId: {}", userId);
                    return new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND);
                });
    }

    /**
     * 공통 메서드: username으로 User 조회
     */
    private User getUserByUsernameOrThrow(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("User not found with username: {}", username);
                    return new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND);
                });
    }

    @Transactional
    public UserInfoResponse signUp(SignUpRequest request) {
        log.info("회원가입 시도: username: {}", request.getUsername());

        if (userRepository.existsByUsername(request.getUsername())) {
            log.warn("회원가입 실패: 이미 존재하는 username - {}", request.getUsername());
            throw new AuthException(ErrorCode.USERNAME_ALREADY_EXISTS, request.getUsername());
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("회원가입 실패: 이미 존재하는 email - {}", request.getEmail());
            throw new AuthException(ErrorCode.EMAIL_ALREADY_EXISTS, request.getEmail());
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .role(User.Role.USER)
                .isActive(true)
                .build();

        User savedUser = userRepository.save(user);
        dashboardProvisioningService.ensureDefaultDashboard(savedUser);
        log.info("회원가입 성공: userId: {}, username: {}", savedUser.getUserId(), savedUser.getUsername());

        return convertToUserInfoResponse(savedUser);
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        log.info("로그인 시도: username: {}", request.getUsername());
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );

            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

            if (!userDetails.isEnabled()) {
                log.warn("로그인 실패: 비활성화된 사용자 - {}", request.getUsername());
                throw new AuthException(ErrorCode.USER_NOT_ACTIVE);
            }

            SecurityContextHolder.getContext().setAuthentication(authentication);

            String accessToken = jwtProvider.generateAccessToken(authentication);
            String refreshToken = jwtProvider.generateRefreshToken(userDetails.getUserId());

            User user = getUserByUsernameOrThrow(request.getUsername());

            LocalDateTime expiryDate = LocalDateTime.now().plus(REFRESH_TOKEN_EXPIRATION_MS, ChronoUnit.MILLIS);

            RefreshToken savedRefreshToken = RefreshToken.builder()
                    .user(user)
                    .token(refreshToken)
                    .expiresAt(expiryDate)
                    .isRevoked(false)
                    .build();

            refreshTokenRepository.save(savedRefreshToken);
            log.info("로그인 성공: username: {}, userId: {}", user.getUsername(), user.getUserId());

            return LoginResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType(BEARER_TOKEN_TYPE)
                    .expiresIn(jwtProvider.getAccessTokenExpirationTime())
                    .userId(user.getUserId())
                    .username(user.getUsername())
                    .role(user.getRole().toString())
                    .build();

        } catch (DisabledException e) {
            log.warn("로그인 실패: username: {} - 비활성화된 계정", request.getUsername());
            throw new AuthException(ErrorCode.USER_NOT_ACTIVE);
        } catch (AuthException | ResourceNotFoundException e) {
            log.warn("로그인 실패: username: {} - {}", request.getUsername(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.warn("로그인 실패: username: {} - 잘못된 자격증명", request.getUsername());
            throw new AuthException(ErrorCode.INVALID_CREDENTIALS);
        }
    }

    @Transactional
    public LoginResponse refreshAccessToken(RefreshTokenRequest request) {
        log.info("Access token 재발급 시도");
        String refreshToken = request.getRefreshToken();

        if (!jwtProvider.validateToken(refreshToken)) {
            log.warn("Token 재발급 실패: 유효하지 않거나 만료된 refresh token");
            throw new AuthException(ErrorCode.INVALID_TOKEN);
        }

        RefreshToken storedToken = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> {
                    log.warn("Token 재발급 실패: Refresh token not found");
                    return new AuthException(ErrorCode.INVALID_TOKEN);
                });

        if (storedToken.getIsRevoked() || storedToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            log.warn("Token 재발급 실패: Refresh token이 revoked되었거나 만료됨");
            throw new AuthException(ErrorCode.INVALID_TOKEN);
        }

        Long userId = jwtProvider.getUserIdFromJWT(refreshToken);
        User user = getUserOrThrow(userId);

        String newAccessToken = jwtProvider.generateAccessTokenFromUserId(
                user.getUserId(),
                user.getUsername(),
                user.getRole().toString()
        );

        log.info("Access token 재발급 성공: username: {}, userId: {}", user.getUsername(), user.getUserId());

        return LoginResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken)
                .tokenType(BEARER_TOKEN_TYPE)
                .expiresIn(jwtProvider.getAccessTokenExpirationTime())
                .userId(user.getUserId())
                .username(user.getUsername())
                .role(user.getRole().toString())
                .build();
    }

    @Transactional
    public void logout() {
        log.info("로그아웃 시도");
        try {
            Long userId = SecurityContextHelper.getCurrentUserId();
            User user = getUserOrThrow(userId);
            refreshTokenRepository.deleteByUser(user);
            log.info("로그아웃 성공: username: {}, userId: {}", user.getUsername(), user.getUserId());
        } catch (Exception e) {
            log.warn("로그아웃 실패: {}", e.getMessage());
            // 로그아웃 실패 시에도 계속 진행 (보안 컨텍스트는 자동으로 클리어됨)
        }
    }

    public UserInfoResponse getUserInfo(Long userId) {
        log.debug("사용자 정보 조회: userId: {}", userId);
        User user = getUserOrThrow(userId);
        return convertToUserInfoResponse(user);
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        log.info("비밀번호 변경 시도: userId: {}", userId);

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            log.warn("비밀번호 변경 실패: userId: {} - 새 비밀번호 확인 불일치", userId);
            throw new ValidationException(ErrorCode.PASSWORD_MISMATCH);
        }

        User user = getUserOrThrow(userId);

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            log.warn("비밀번호 변경 실패: userId: {} - 현재 비밀번호 불일치", userId);
            throw new AuthException(ErrorCode.INVALID_CREDENTIALS);
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            log.warn("비밀번호 변경 실패: userId: {} - 새 비밀번호가 현재 비밀번호와 동일", userId);
            throw new ValidationException(ErrorCode.PASSWORD_SAME_AS_CURRENT);
        }

        // 새 비밀번호로 암호화하여 저장
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        log.info("비밀번호 변경 성공: userId: {}", userId);
    }

    /**
     * 사용자의 토큰 통계 조회
     */
    public TokenStatsResponse getTokenStats(Long userId) {
        log.debug("토큰 통계 조회: userId: {}", userId);
        User user = getUserOrThrow(userId);

        LocalDateTime now = LocalDateTime.now();
        long activeTokenCount = refreshTokenRepository.countActiveTokensByUser(user, now);
        long expiredTokenCount = refreshTokenRepository.countExpiredTokensByUser(user, now);
        long revokedTokenCount = refreshTokenRepository.countRevokedTokensByUser(user);
        LocalDateTime lastTokenCreatedAt = refreshTokenRepository.findLastTokenCreatedAtByUser(user);

        return TokenStatsResponse.builder()
                .userId(userId)
                .activeTokenCount(activeTokenCount)
                .expiredTokenCount(expiredTokenCount)
                .revokedTokenCount(revokedTokenCount)
                .totalTokenCount(activeTokenCount + expiredTokenCount + revokedTokenCount)
                .lastTokenCreatedAt(lastTokenCreatedAt)
                .build();
    }

    /**
     * 사용자의 만료된 토큰 정리
     */
    @Transactional
    public long cleanupExpiredTokens(Long userId) {
        log.info("만료된 토큰 정리: userId: {}", userId);
        User user = getUserOrThrow(userId);

        List<RefreshToken> expiredTokens = refreshTokenRepository.findAll().stream()
                .filter(token -> token.getUser().equals(user) && token.getExpiresAt().isBefore(LocalDateTime.now()))
                .toList();

        long count = expiredTokens.size();
        refreshTokenRepository.deleteAll(expiredTokens);

        log.info("만료된 토큰 정리 완료: userId: {}, count: {}", userId, count);
        return count;
    }

    /**
     * 사용자의 모든 토큰 폐지 (로그아웃 유사)
     */
    @Transactional
    public void revokeAllTokens(Long userId) {
        log.info("모든 토큰 폐지: userId: {}", userId);
        User user = getUserOrThrow(userId);

        List<RefreshToken> tokens = refreshTokenRepository.findActiveTokensByUser(user);
        tokens.forEach(token -> token.setIsRevoked(true));
        refreshTokenRepository.saveAll(tokens);

        log.info("모든 토큰 폐지 완료: userId: {}", userId);
    }

    /**
     * User 엔티티를 UserInfoResponse DTO로 변환
     */
    private UserInfoResponse convertToUserInfoResponse(User user) {
        return UserInfoResponse.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().toString())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
