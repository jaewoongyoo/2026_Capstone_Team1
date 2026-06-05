// 인증 컨트롤러 토큰 통계 테스트
package com.festapp.dashboard.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.festapp.dashboard.common.security.JwtProvider;
import com.festapp.dashboard.config.IntegrationTestConfig;
import com.festapp.dashboard.user.entity.User;
import com.festapp.dashboard.user.repository.UserRepository;
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
@DisplayName("AuthController 토큰 통계 API 테스트")
@Transactional
public class AuthControllerTokenStatsTest {

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
    private Long testUserId;
    private String testUserToken;

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
                .password(passwordEncoder.encode("Password123!"))
                .fullName("Test User")
                .role(User.Role.USER)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testUser = userRepository.save(testUser);
        testUserId = testUser.getUserId();

        // JWT 토큰 생성
        testUserToken = jwtProvider.generateAccessTokenFromUserId(
                testUser.getUserId(),
                testUser.getUsername(),
                testUser.getRole().toString()
        );
    }

    @Test
    @DisplayName("GET /api/auth/token-stats - 토큰 통계 조회")
    void testGetTokenStats() throws Exception {
        mockMvc.perform(get("/api/auth/token-stats")
                .header("Authorization", "Bearer " + testUserToken)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(testUserId))
                .andExpect(jsonPath("$.data.activeTokenCount").isNumber())
                .andExpect(jsonPath("$.data.expiredTokenCount").isNumber())
                .andExpect(jsonPath("$.data.revokedTokenCount").isNumber())
                .andExpect(jsonPath("$.data.totalTokenCount").isNumber());
    }

    @Test
    @DisplayName("POST /api/auth/token-stats/cleanup - 만료된 토큰 정리")
    void testCleanupExpiredTokens() throws Exception {
        mockMvc.perform(post("/api/auth/token-stats/cleanup")
                .header("Authorization", "Bearer " + testUserToken)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isNumber());
    }

    @Test
    @DisplayName("POST /api/auth/revoke-all-tokens - 모든 토큰 폐지")
    void testRevokeAllTokens() throws Exception {
        mockMvc.perform(post("/api/auth/revoke-all-tokens")
                .header("Authorization", "Bearer " + testUserToken)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("모든 토큰이 폐지되었습니다"));
    }

    @Test
    @DisplayName("토큰 통계 조회 - 인증 필요")
    void testGetTokenStatsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/auth/token-stats")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }
}

