package com.festapp.dashboard.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.festapp.dashboard.common.security.JwtProvider;
import com.festapp.dashboard.dashboard.dto.DashboardRequest;
import com.festapp.dashboard.dashboard.entity.Dashboard;
import com.festapp.dashboard.dashboard.repository.DashboardRepository;
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
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
@Import(IntegrationTestConfig.class)
@DisplayName("Dashboard Share API 통합 테스트")
@Transactional
public class DashboardShareIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DashboardRepository dashboardRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtProvider jwtProvider;

    private MockMvc mockMvc;
    private User testUser;
    private String testAccessToken;
    private Long testDashboardId;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        dashboardRepository.deleteAll();
        userRepository.deleteAll();

        // 1. 테스트 유저 생성
        testUser = User.builder()
                .username("shareuser")
                .email("share@example.com")
                .password(passwordEncoder.encode("SharePass123!"))
                .fullName("Share User")
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

        // 2. 대시보드 생성
        DashboardRequest dashboardRequest = DashboardRequest.builder()
                .dashboardName("My Shared Dashboard")
                .description("Test Description")
                .build();

        String createResponse = mockMvc.perform(post("/api/dashboards")
                        .header("Authorization", "Bearer " + testAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dashboardRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        testDashboardId = objectMapper.readTree(createResponse).path("data").path("dashboardId").asLong();
    }

    @Test
    @DisplayName("공유 활성화 및 비활성화, 외부 조회 정상 흐름 테스트")
    void testDashboardShareFlow() throws Exception {
        // [Step 1] 기본 대시보드는 공유가 비활성화 상태여야 함 (shareToken null, isPublic false)
        mockMvc.perform(get("/api/dashboards/" + testDashboardId)
                        .header("Authorization", "Bearer " + testAccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isPublic").value(false))
                .andExpect(jsonPath("$.data.shareToken").value(nullValue()));

        // [Step 2] 대시보드 공유 활성화
        String enableResponse = mockMvc.perform(post("/api/dashboards/" + testDashboardId + "/share/enable")
                        .header("Authorization", "Bearer " + testAccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.isPublic").value(true))
                .andExpect(jsonPath("$.data.shareToken").value(notNullValue()))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String shareToken = objectMapper.readTree(enableResponse).path("data").path("shareToken").asText();

        // [Step 3] 비로그인(외부) 상태에서 shareToken을 통해 대시보드 조회
        mockMvc.perform(get("/api/public/dashboards")
                        .param("token", shareToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.dashboardName").value("My Shared Dashboard"))
                .andExpect(jsonPath("$.data.isPublic").value(true))
                .andExpect(jsonPath("$.data.shareToken").value(shareToken))
                .andExpect(jsonPath("$.data.widgets").isArray())
                .andExpect(jsonPath("$.data.equipmentCurrent").isArray());

        // [Step 4] 대시보드 공유 비활성화
        mockMvc.perform(post("/api/dashboards/" + testDashboardId + "/share/disable")
                        .header("Authorization", "Bearer " + testAccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.isPublic").value(false))
                .andExpect(jsonPath("$.data.shareToken").value(nullValue()));

        // [Step 5] 비로그인 상태에서 비활성화된 token으로 대시보드 조회 시 404 (Not Found) 반환 검증
        mockMvc.perform(get("/api/public/dashboards")
                        .param("token", shareToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }
}
