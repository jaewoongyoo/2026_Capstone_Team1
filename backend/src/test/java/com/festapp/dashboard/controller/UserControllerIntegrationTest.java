// 사용자 컨트롤러 통합 테스트 - API 엔드포인트 검증
package com.festapp.dashboard.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.festapp.dashboard.common.security.JwtProvider;
import com.festapp.dashboard.user.dto.UserCreateRequest;
import com.festapp.dashboard.user.dto.UserUpdateRequest;
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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
@Import(IntegrationTestConfig.class)
@DisplayName("UserController 통합 테스트")
@Transactional
public class UserControllerIntegrationTest {

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
    private String adminToken;

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

        // JWT 토큰 생성 - USER 권한
        testUserToken = jwtProvider.generateAccessTokenFromUserId(
                testUser.getUserId(),
                testUser.getUsername(),
                testUser.getRole().toString()
        );

        // ADMIN 토큰 생성을 위해 ADMIN 사용자 생성
        User adminUser = User.builder()
                .username("adminuser")
                .email("admin@example.com")
                .password(passwordEncoder.encode("AdminPass123!"))
                .fullName("Admin User")
                .role(User.Role.ADMIN)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        adminUser = userRepository.save(adminUser);

        adminToken = jwtProvider.generateAccessTokenFromUserId(
                adminUser.getUserId(),
                adminUser.getUsername(),
                adminUser.getRole().toString()
        );
    }

    @Test
    @DisplayName("POST /api/users - 사용자 생성 성공")
    void testCreateUserSuccess() throws Exception {
        UserCreateRequest createRequest = UserCreateRequest.builder()
                .username("newuser")
                .email("newuser@example.com")
                .password("NewPass123!")
                .fullName("New User")
                .role("USER")
                .build();

        mockMvc.perform(post("/api/users")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.username").value("newuser"))
                .andExpect(jsonPath("$.data.email").value("newuser@example.com"))
                .andExpect(jsonPath("$.data.role").value("USER"));
    }

    @Test
    @DisplayName("POST /api/users - 사용자 생성 실패 - 중복된 사용자명")
    void testCreateUserFailureDuplicateUsername() throws Exception {
        UserCreateRequest createRequest = UserCreateRequest.builder()
                .username("testuser")
                .email("newemail@example.com")
                .password("NewPass123!")
                .fullName("New User")
                .role("USER")
                .build();

        mockMvc.perform(post("/api/users")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/users - 사용자 생성 실패 - 유효하지 않은 입력")
    void testCreateUserFailureInvalidInput() throws Exception {
        UserCreateRequest createRequest = UserCreateRequest.builder()
                .username("ab")  // 너무 짧음
                .email("newemail@example.com")
                .password("NewPass123!")
                .fullName("New User")
                .role("USER")
                .build();

        mockMvc.perform(post("/api/users")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/users/{userId} - 사용자 조회 성공")
    void testGetUserByIdSuccess() throws Exception {
        mockMvc.perform(get("/api/users/{userId}", testUserId)
                .header("Authorization", "Bearer " + testUserToken)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(testUserId))
                .andExpect(jsonPath("$.data.username").value("testuser"))
                .andExpect(jsonPath("$.data.email").value("test@example.com"));
    }

    @Test
    @DisplayName("GET /api/users/{userId} - 존재하지 않는 사용자")
    void testGetUserByIdNotFound() throws Exception {
        mockMvc.perform(get("/api/users/{userId}", 999L)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value(4002));
    }

    @Test
    @DisplayName("GET /api/users/username/{username} - 사용자명으로 조회 성공")
    void testGetUserByUsernameSuccess() throws Exception {
        mockMvc.perform(get("/api/users/username/{username}", "testuser")
                .header("Authorization", "Bearer " + testUserToken)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("testuser"))
                .andExpect(jsonPath("$.data.email").value("test@example.com"));
    }

    @Test
    @DisplayName("GET /api/users - 모든 사용자 조회 (ADMIN 권한 필요)")
    void testGetAllUsersSuccess() throws Exception {
        mockMvc.perform(get("/api/users")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data[0].username").exists());
    }

    @Test
    @DisplayName("GET /api/users - 권한 없음 (USER 권한)")
    void testGetAllUsersForbidden() throws Exception {
        mockMvc.perform(get("/api/users")
                .header("Authorization", "Bearer " + testUserToken)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/users/search - 사용자 검색 성공")
    void testSearchUsersSuccess() throws Exception {
        mockMvc.perform(get("/api/users/search")
                .header("Authorization", "Bearer " + adminToken)
                .param("keyword", "test")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data[0].username").value("testuser"));
    }

    @Test
    @DisplayName("PUT /api/users/{userId} - 사용자 정보 수정 성공")
    void testUpdateUserSuccess() throws Exception {
        UserUpdateRequest updateRequest = UserUpdateRequest.builder()
                .fullName("Updated Name")
                .email("newemail@example.com")
                .build();

        mockMvc.perform(put("/api/users/{userId}", testUserId)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fullName").value("Updated Name"))
                .andExpect(jsonPath("$.data.email").value("newemail@example.com"));
    }

    @Test
    @DisplayName("PUT /api/users/{userId} - 유효하지 않은 이메일 형식")
    void testUpdateUserInvalidEmail() throws Exception {
        UserUpdateRequest updateRequest = UserUpdateRequest.builder()
                .email("invalid-email")
                .build();

        mockMvc.perform(put("/api/users/{userId}", testUserId)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/users/{userId}/activate - 사용자 활성화")
    void testActivateUserSuccess() throws Exception {
        // 먼저 사용자 비활성화
        testUser.setIsActive(false);
        userRepository.save(testUser);

        mockMvc.perform(post("/api/users/{userId}/activate", testUserId)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("POST /api/users/{userId}/deactivate - 사용자 비활성화")
    void testDeactivateUserSuccess() throws Exception {
        mockMvc.perform(post("/api/users/{userId}/deactivate", testUserId)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("POST /api/users/{userId}/promote - 관리자로 승격")
    void testPromoteToAdminSuccess() throws Exception {
        mockMvc.perform(post("/api/users/{userId}/promote", testUserId)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("POST /api/users/{userId}/demote - 일반 사용자로 강등")
    void testDemoteToUserSuccess() throws Exception {
        // 먼저 관리자로 승격
        testUser.setRole(User.Role.ADMIN);
        userRepository.save(testUser);

        mockMvc.perform(post("/api/users/{userId}/demote", testUserId)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("DELETE /api/users/{userId} - 사용자 삭제")
    void testDeleteUserSuccess() throws Exception {
        mockMvc.perform(delete("/api/users/{userId}", testUserId)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("DELETE /api/users/{userId} - 인증 필요")
    void testDeleteUserUnauthorized() throws Exception {
        mockMvc.perform(delete("/api/users/{userId}", testUserId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/users/stats/count-by-role/{role} - 역할별 사용자 수 조회")
    void testGetUserCountByRole() throws Exception {
        mockMvc.perform(get("/api/users/stats/count-by-role/USER")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isNumber())
                .andExpect(jsonPath("$.data").value(greaterThanOrEqualTo(1)));
    }

    @Test
    @DisplayName("GET /api/users/stats/count-active - 활성 사용자 수 조회")
    void testGetActiveUserCount() throws Exception {
        mockMvc.perform(get("/api/users/stats/count-active")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isNumber())
                .andExpect(jsonPath("$.data").value(greaterThanOrEqualTo(1)));
    }

    @Test
    @DisplayName("GET /api/users/stats/count-inactive - 비활성 사용자 수 조회")
    void testGetInactiveUserCount() throws Exception {
        mockMvc.perform(get("/api/users/stats/count-inactive")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isNumber());
    }
}

