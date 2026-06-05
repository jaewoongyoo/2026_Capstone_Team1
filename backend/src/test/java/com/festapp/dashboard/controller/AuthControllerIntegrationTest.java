// 인증 컨트롤러 통합 테스트 - 인증 API 엔드포인트 검증
package com.festapp.dashboard.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.festapp.dashboard.auth.dto.*;
import com.festapp.dashboard.common.security.JwtProvider;
import com.festapp.dashboard.user.entity.User;
import com.festapp.dashboard.user.repository.UserRepository;
import com.festapp.dashboard.config.IntegrationTestConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
@Import(IntegrationTestConfig.class)
@DisplayName("AuthController 통합 테스트")
@Transactional
public class AuthControllerIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtProvider jwtProvider;

    private MockMvc mockMvc;
    private User testUser;
    private String testAccessToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        userRepository.deleteAll();

        testUser = User.builder()
                .username("testuser")
                .email("test@example.com")
                .password(passwordEncoder.encode("TestPass123!"))
                .fullName("Test User")
                .role(User.Role.USER)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testUser = userRepository.save(testUser);

        // JWT 토큰 생성
        testAccessToken = jwtProvider.generateAccessTokenFromUserId(
                testUser.getUserId(),
                testUser.getUsername(),
                testUser.getRole().toString()
        );
    }

    @Test
    @DisplayName("회원가입 성공")
    void testSignUpSuccess() throws Exception {
        SignUpRequest request = SignUpRequest.builder()
                .username("newuser")
                .email("newuser@example.com")
                .password("NewPass123!")
                .fullName("New User")
                .build();

        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("newuser"))
                .andExpect(jsonPath("$.data.email").value("newuser@example.com"));
    }

    @Test
    @DisplayName("회원가입 실패 - 이미 존재하는 username")
    void testSignUpFailureUsernameExists() throws Exception {
        SignUpRequest request = SignUpRequest.builder()
                .username("testuser")
                .email("another@example.com")
                .password("NewPass123!")
                .fullName("Another User")
                .build();

        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value(4005));
    }

    @Test
    @DisplayName("회원가입 실패 - 유효하지 않은 이메일")
    void testSignUpFailureInvalidEmail() throws Exception {
        SignUpRequest request = SignUpRequest.builder()
                .username("newuser")
                .email("invalid-email")
                .password("NewPass123!")
                .fullName("New User")
                .build();

        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("로그인 성공")
    void testLoginSuccess() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .username("testuser")
                .password("TestPass123!")
                .build();

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.data.userId").value(testUser.getUserId()))
                .andExpect(jsonPath("$.data.username").value("testuser"));
    }

    @Test
    @DisplayName("로그인 실패 - 잘못된 비밀번호")
    void testLoginFailureWrongPassword() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .username("testuser")
                .password("WrongPassword123!")
                .build();

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value(4001));
    }

    @Test
    @DisplayName("로그인 실패 - 존재하지 않는 사용자")
    void testLoginFailureUserNotFound() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .username("nonexistent")
                .password("TestPass123!")
                .build();

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value(4001));
    }

    @Test
    @DisplayName("내 정보 조회 성공")
    void testGetMeSuccess() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                .header("Authorization", "Bearer " + testAccessToken)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("testuser"))
                .andExpect(jsonPath("$.data.email").value("test@example.com"));
    }

    @Test
    @DisplayName("내 정보 조회 실패 - 인증 필요")
    void testGetMeUnauthorized() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("비밀번호 변경 성공")
    void testChangePasswordSuccess() throws Exception {
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("TestPass123!")
                .newPassword("NewPass456!")
                .confirmPassword("NewPass456!")
                .build();

        mockMvc.perform(post("/api/auth/change-password")
                .header("Authorization", "Bearer " + testAccessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("비밀번호 변경 실패 - 비밀번호 확인 불일치")
    void testChangePasswordFailureMismatch() throws Exception {
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("TestPass123!")
                .newPassword("NewPass456!")
                .confirmPassword("DifferentPass456!")
                .build();

        mockMvc.perform(post("/api/auth/change-password")
                .header("Authorization", "Bearer " + testAccessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("비밀번호 변경 실패 - 현재 비밀번호 불일치")
    void testChangePasswordFailureWrongCurrentPassword() throws Exception {
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("WrongPassword!")
                .newPassword("NewPass456!")
                .confirmPassword("NewPass456!")
                .build();

        mockMvc.perform(post("/api/auth/change-password")
                .header("Authorization", "Bearer " + testAccessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value(4001));
    }

    @Test
    @DisplayName("로그아웃 성공")
    void testLogoutSuccess() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                .header("Authorization", "Bearer " + testAccessToken)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("로그아웃 실패 - 인증 필요")
    void testLogoutUnauthorized() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("회원가입 실패 - 필수 필드 누락")
    void testSignUpFailureMissingFields() throws Exception {
        SignUpRequest request = SignUpRequest.builder()
                .username("newuser")
                // email 누락
                .password("NewPass123!")
                .build();

        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("회원가입 실패 - 약한 비밀번호")
    void testSignUpFailureWeakPassword() throws Exception {
        SignUpRequest request = SignUpRequest.builder()
                .username("newuser")
                .email("newuser@example.com")
                .password("weak") // 약한 비밀번호
                .fullName("New User")
                .build();

        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("로그인 실패 - 비활성화된 사용자")
    void testLoginFailureInactiveUser() throws Exception {
        // 사용자 비활성화
        testUser.setIsActive(false);
        userRepository.save(testUser);

        LoginRequest request = LoginRequest.builder()
                .username("testuser")
                .password("TestPass123!")
                .build();

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value(4003));
    }
}

