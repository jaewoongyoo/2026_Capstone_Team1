// 인증 서비스 테스트 - AuthService의 비즈니스 로직 검증
package com.festapp.dashboard.service;

import com.festapp.dashboard.auth.dto.*;
import com.festapp.dashboard.auth.entity.RefreshToken;
import com.festapp.dashboard.auth.exception.AuthException;
import com.festapp.dashboard.auth.repository.RefreshTokenRepository;
import com.festapp.dashboard.auth.service.AuthService;
import com.festapp.dashboard.dashboard.service.DashboardProvisioningService;
import com.festapp.dashboard.user.dto.UserInfoResponse;
import com.festapp.dashboard.user.entity.User;
import com.festapp.dashboard.user.repository.UserRepository;
import com.festapp.dashboard.common.exception.ResourceNotFoundException;
import com.festapp.dashboard.common.exception.ValidationException;
import com.festapp.dashboard.common.security.CustomUserDetails;
import com.festapp.dashboard.common.security.JwtProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService 테스트")
public class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private DashboardProvisioningService dashboardProvisioningService;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private SignUpRequest signUpRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .userId(1L)
                .username("testuser")
                .email("test@example.com")
                .password("hashedPassword123")
                .fullName("Test User")
                .role(User.Role.USER)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        signUpRequest = SignUpRequest.builder()
                .username("newuser")
                .email("newuser@example.com")
                .password("NewPass123!")
                .fullName("New User")
                .build();

        loginRequest = LoginRequest.builder()
                .username("testuser")
                .password("TestPass123!")
                .build();
    }

    @Test
    @DisplayName("회원가입 성공")
    void testSignUpSuccess() {
        // Given
        when(userRepository.existsByUsername(signUpRequest.getUsername())).thenReturn(false);
        when(userRepository.existsByEmail(signUpRequest.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(signUpRequest.getPassword())).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(User.builder()
                .userId(2L)
                .username(signUpRequest.getUsername())
                .email(signUpRequest.getEmail())
                .password("hashedPassword")
                .fullName(signUpRequest.getFullName())
                .role(User.Role.USER)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build());

        // When
        UserInfoResponse response = authService.signUp(signUpRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getUsername()).isEqualTo(signUpRequest.getUsername());
        assertThat(response.getEmail()).isEqualTo(signUpRequest.getEmail());
        verify(userRepository, times(1)).existsByUsername(signUpRequest.getUsername());
        verify(userRepository, times(1)).existsByEmail(signUpRequest.getEmail());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("회원가입 실패 - 이미 존재하는 username")
    void testSignUpFailureUsernameExists() {
        // Given
        when(userRepository.existsByUsername(signUpRequest.getUsername())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> authService.signUp(signUpRequest))
                .isInstanceOf(AuthException.class);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("회원가입 실패 - 이미 존재하는 email")
    void testSignUpFailureEmailExists() {
        // Given
        when(userRepository.existsByUsername(signUpRequest.getUsername())).thenReturn(false);
        when(userRepository.existsByEmail(signUpRequest.getEmail())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> authService.signUp(signUpRequest))
                .isInstanceOf(AuthException.class);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("로그인 성공")
    void testLoginSuccess() {
        // Given
        CustomUserDetails userDetails = CustomUserDetails.builder()
                .userId(testUser.getUserId())
                .username(testUser.getUsername())
                .password(testUser.getPassword())
                .role(testUser.getRole().toString())
                .isActive(testUser.getIsActive())
                .build();

        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null);

        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(jwtProvider.generateAccessToken(authentication)).thenReturn("accessToken123");
        when(jwtProvider.generateRefreshToken(testUser.getUserId())).thenReturn("refreshToken123");
        when(userRepository.findByUsername(loginRequest.getUsername())).thenReturn(Optional.of(testUser));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(RefreshToken.builder()
                .tokenId(1L)
                .user(testUser)
                .token("refreshToken123")
                .expiresAt(LocalDateTime.now().plusDays(7))
                .isRevoked(false)
                .build());
        when(jwtProvider.getAccessTokenExpirationTime()).thenReturn(3600000L);

        // When
        LoginResponse response = authService.login(loginRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("accessToken123");
        assertThat(response.getRefreshToken()).isEqualTo("refreshToken123");
        assertThat(response.getUserId()).isEqualTo(testUser.getUserId());
        verify(authenticationManager, times(1)).authenticate(any());
        verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("로그인 실패 - 비활성화된 사용자")
    void testLoginFailureInactiveUser() {
        // Given
        testUser.setIsActive(false);
        CustomUserDetails userDetails = CustomUserDetails.builder()
                .userId(testUser.getUserId())
                .username(testUser.getUsername())
                .isActive(false)
                .build();

        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null);
        when(authenticationManager.authenticate(any())).thenReturn(authentication);

        // When & Then
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(AuthException.class);
    }

    @Test
    @DisplayName("로그인 실패 - 잘못된 자격증명")
    void testLoginFailureInvalidCredentials() {
        // Given
        when(authenticationManager.authenticate(any())).thenThrow(new org.springframework.security.core.AuthenticationException("Invalid credentials") {});

        // When & Then
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(AuthException.class);
    }

    @Test
    @DisplayName("토큰 재발급 성공")
    void testRefreshAccessTokenSuccess() {
        // Given
        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("validRefreshToken")
                .build();

        RefreshToken storedToken = RefreshToken.builder()
                .tokenId(1L)
                .user(testUser)
                .token("validRefreshToken")
                .expiresAt(LocalDateTime.now().plusDays(1))
                .isRevoked(false)
                .build();

        when(jwtProvider.validateToken("validRefreshToken")).thenReturn(true);
        when(refreshTokenRepository.findByToken("validRefreshToken")).thenReturn(Optional.of(storedToken));
        when(jwtProvider.getUserIdFromJWT("validRefreshToken")).thenReturn(testUser.getUserId());
        when(userRepository.findById(testUser.getUserId())).thenReturn(Optional.of(testUser));
        when(jwtProvider.generateAccessTokenFromUserId(anyLong(), anyString(), anyString())).thenReturn("newAccessToken");
        when(jwtProvider.getAccessTokenExpirationTime()).thenReturn(3600000L);

        // When
        LoginResponse response = authService.refreshAccessToken(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("newAccessToken");
        assertThat(response.getRefreshToken()).isEqualTo("validRefreshToken");
        verify(jwtProvider, times(1)).validateToken("validRefreshToken");
    }

    @Test
    @DisplayName("토큰 재발급 실패 - 유효하지 않은 토큰")
    void testRefreshAccessTokenFailureInvalidToken() {
        // Given
        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("invalidRefreshToken")
                .build();

        when(jwtProvider.validateToken("invalidRefreshToken")).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> authService.refreshAccessToken(request))
                .isInstanceOf(AuthException.class);
    }

    @Test
    @DisplayName("토큰 재발급 실패 - 만료된 토큰")
    void testRefreshAccessTokenFailureExpiredToken() {
        // Given
        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("expiredRefreshToken")
                .build();

        RefreshToken storedToken = RefreshToken.builder()
                .tokenId(1L)
                .user(testUser)
                .token("expiredRefreshToken")
                .expiresAt(LocalDateTime.now().minusDays(1))
                .isRevoked(false)
                .build();

        when(jwtProvider.validateToken("expiredRefreshToken")).thenReturn(true);
        when(refreshTokenRepository.findByToken("expiredRefreshToken")).thenReturn(Optional.of(storedToken));

        // When & Then
        assertThatThrownBy(() -> authService.refreshAccessToken(request))
                .isInstanceOf(AuthException.class);
    }

    @Test
    @DisplayName("비밀번호 변경 성공")
    void testChangePasswordSuccess() {
        // Given
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("OldPass123!")
                .newPassword("NewPass456!")
                .confirmPassword("NewPass456!")
                .build();

        when(userRepository.findById(testUser.getUserId())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("OldPass123!", "hashedPassword123")).thenReturn(true);
        when(passwordEncoder.matches("NewPass456!", "hashedPassword123")).thenReturn(false);
        when(passwordEncoder.encode("NewPass456!")).thenReturn("hashedNewPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        authService.changePassword(testUser.getUserId(), request);

        // Then
        verify(userRepository, times(1)).findById(testUser.getUserId());
        verify(passwordEncoder, times(1)).matches("OldPass123!", "hashedPassword123");
        verify(passwordEncoder, times(1)).matches("NewPass456!", "hashedPassword123");
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("비밀번호 변경 실패 - 비밀번호 확인 불일치")
    void testChangePasswordFailureConfirmMismatch() {
        // Given
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("OldPass123!")
                .newPassword("NewPass456!")
                .confirmPassword("DifferentPass456!")
                .build();


        // When & Then
        assertThatThrownBy(() -> authService.changePassword(testUser.getUserId(), request))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("비밀번호 변경 실패 - 현재 비밀번호 불일치")
    void testChangePasswordFailureWrongCurrentPassword() {
        // Given
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("WrongPassword!")
                .newPassword("NewPass456!")
                .confirmPassword("NewPass456!")
                .build();

        when(userRepository.findById(testUser.getUserId())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(request.getCurrentPassword(), testUser.getPassword())).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> authService.changePassword(testUser.getUserId(), request))
                .isInstanceOf(AuthException.class);
    }

    @Test
    @DisplayName("비밀번호 변경 실패 - 새 비밀번호가 현재와 동일")
    void testChangePasswordFailureSamePassword() {
        // Given
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("OldPass123!")
                .newPassword("OldPass123!")
                .confirmPassword("OldPass123!")
                .build();

        when(userRepository.findById(testUser.getUserId())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(request.getCurrentPassword(), testUser.getPassword())).thenReturn(true);
        when(passwordEncoder.matches(request.getNewPassword(), testUser.getPassword())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> authService.changePassword(testUser.getUserId(), request))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("사용자 정보 조회 성공")
    void testGetUserInfoSuccess() {
        // Given
        when(userRepository.findById(testUser.getUserId())).thenReturn(Optional.of(testUser));

        // When
        UserInfoResponse response = authService.getUserInfo(testUser.getUserId());

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getUserId()).isEqualTo(testUser.getUserId());
        assertThat(response.getUsername()).isEqualTo(testUser.getUsername());
        assertThat(response.getEmail()).isEqualTo(testUser.getEmail());
        verify(userRepository, times(1)).findById(testUser.getUserId());
    }

    @Test
    @DisplayName("사용자 정보 조회 실패 - 사용자 없음")
    void testGetUserInfoNotFound() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> authService.getUserInfo(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}

