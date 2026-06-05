// 대시보드 위젯 API 통합 테스트 - Widget API 엔드포인트 검증
package com.festapp.dashboard.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.festapp.dashboard.dashboard.widget.dto.WidgetLayoutUpdateDto;
import com.festapp.dashboard.dashboard.widget.dto.WidgetRequestDto;
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
import java.util.Arrays;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
@Import(IntegrationTestConfig.class)
@DisplayName("DashboardWidget API 통합 테스트")
@Transactional
public class DashboardWidgetAPIIntegrationTest {

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
    private Long widgetId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        userRepository.deleteAll();

        testUser = User.builder()
                .username("widgetuser")
                .email("widget@example.com")
                .password(passwordEncoder.encode("WidgetPass123!"))
                .fullName("Widget User")
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
    @DisplayName("위젯 생성 성공")
    void testCreateWidgetSuccess() throws Exception {
        WidgetRequestDto request = WidgetRequestDto.builder()
                .equipmentId("CVD-CHAMBER-01")
                .widgetType("GAUGE")
                .title("Temperature Gauge")
                .sensorId("Temp_Sensor_001")
                .chartType("line")
                .dataType("FLOAT")
                .unit("°C")
                .posX(0)
                .posY(0)
                .width(2)
                .height(2)
                .configJson("{\"min\": 0, \"max\": 1000}")
                .build();

        mockMvc.perform(post("/api/dashboard/widgets")
                .header("Authorization", "Bearer " + testAccessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Temperature Gauge"))
                .andExpect(jsonPath("$.data.equipmentId").value("CVD-CHAMBER-01"))
                .andExpect(jsonPath("$.data.posX").value(0));
    }

    @Test
    @DisplayName("위젯 생성 실패 - 인증 필요")
    void testCreateWidgetUnauthorized() throws Exception {
        WidgetRequestDto request = WidgetRequestDto.builder()
                .equipmentId("CVD-CHAMBER-01")
                .widgetType("GAUGE")
                .title("Temperature Gauge")
                .posX(0)
                .posY(0)
                .width(2)
                .height(2)
                .build();

        mockMvc.perform(post("/api/dashboard/widgets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("위젯 생성 실패 - 필수 필드 누락")
    void testCreateWidgetFailureMissingFields() throws Exception {
        WidgetRequestDto request = WidgetRequestDto.builder()
                .widgetType("GAUGE")
                .title("Temperature Gauge")
                // equipmentId 누락
                .posX(0)
                .posY(0)
                .width(2)
                .height(2)
                .build();

        mockMvc.perform(post("/api/dashboard/widgets")
                .header("Authorization", "Bearer " + testAccessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("위젯 생성 실패 - 음수 좌표")
    void testCreateWidgetFailureNegativePosition() throws Exception {
        WidgetRequestDto request = WidgetRequestDto.builder()
                .equipmentId("CVD-CHAMBER-01")
                .widgetType("GAUGE")
                .title("Temperature Gauge")
                .posX(-1) // 음수 좌표
                .posY(0)
                .width(2)
                .height(2)
                .build();

        mockMvc.perform(post("/api/dashboard/widgets")
                .header("Authorization", "Bearer " + testAccessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("위젯 생성 실패 - 잘못된 크기")
    void testCreateWidgetFailureInvalidSize() throws Exception {
        WidgetRequestDto request = WidgetRequestDto.builder()
                .equipmentId("CVD-CHAMBER-01")
                .widgetType("GAUGE")
                .title("Temperature Gauge")
                .posX(0)
                .posY(0)
                .width(0) // 잘못된 크기
                .height(2)
                .build();

        mockMvc.perform(post("/api/dashboard/widgets")
                .header("Authorization", "Bearer " + testAccessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("내 위젯 전체 조회 성공")
    void testGetMyWidgetsSuccess() throws Exception {
        // 먼저 위젯 생성
        WidgetRequestDto request = WidgetRequestDto.builder()
                .equipmentId("CVD-CHAMBER-01")
                .widgetType("GAUGE")
                .title("Temperature Gauge")
                .posX(0)
                .posY(0)
                .width(2)
                .height(2)
                .build();

        mockMvc.perform(post("/api/dashboard/widgets")
                .header("Authorization", "Bearer " + testAccessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // 위젯 조회
        mockMvc.perform(get("/api/dashboard/widgets")
                .header("Authorization", "Bearer " + testAccessToken)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @DisplayName("장비별 위젯 조회 성공")
    void testGetWidgetsByEquipmentSuccess() throws Exception {
        // 먼저 위젯 생성
        WidgetRequestDto request = WidgetRequestDto.builder()
                .equipmentId("CVD-CHAMBER-01")
                .widgetType("GAUGE")
                .title("Temperature Gauge")
                .posX(0)
                .posY(0)
                .width(2)
                .height(2)
                .build();

        mockMvc.perform(post("/api/dashboard/widgets")
                .header("Authorization", "Bearer " + testAccessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // 장비별 조회
        mockMvc.perform(get("/api/dashboard/widgets/equipment/CVD-CHAMBER-01")
                .header("Authorization", "Bearer " + testAccessToken)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @DisplayName("위젯 단건 조회 성공")
    void testGetWidgetByIdSuccess() throws Exception {
        // 먼저 위젯 생성
        WidgetRequestDto createRequest = WidgetRequestDto.builder()
                .equipmentId("CVD-CHAMBER-01")
                .widgetType("GAUGE")
                .title("Temperature Gauge")
                .posX(0)
                .posY(0)
                .width(2)
                .height(2)
                .build();

        String createResponse = mockMvc.perform(post("/api/dashboard/widgets")
                .header("Authorization", "Bearer " + testAccessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // 응답에서 widgetId 추출
        Long createdWidgetId = objectMapper.readTree(createResponse).path("data").path("id").asLong();

        // 단건 조회
        mockMvc.perform(get("/api/dashboard/widgets/" + createdWidgetId)
                .header("Authorization", "Bearer " + testAccessToken)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Temperature Gauge"));
    }

    @Test
    @DisplayName("위젯 조회 실패 - 존재하지 않는 위젯")
    void testGetWidgetNotFound() throws Exception {
        mockMvc.perform(get("/api/dashboard/widgets/999")
                .header("Authorization", "Bearer " + testAccessToken)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value(4201));
    }

    @Test
    @DisplayName("위젯 수정 성공")
    void testUpdateWidgetSuccess() throws Exception {
        // 먼저 위젯 생성
        WidgetRequestDto createRequest = WidgetRequestDto.builder()
                .equipmentId("CVD-CHAMBER-01")
                .widgetType("GAUGE")
                .title("Temperature Gauge")
                .posX(0)
                .posY(0)
                .width(2)
                .height(2)
                .build();

        String createResponse = mockMvc.perform(post("/api/dashboard/widgets")
                .header("Authorization", "Bearer " + testAccessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // 응답에서 widgetId 추출
        Long createdWidgetId = objectMapper.readTree(createResponse).path("data").path("id").asLong();

        // 위젯 수정
        WidgetRequestDto updateRequest = createRequest.toBuilder()
                .title("Updated Temperature Gauge")
                .build();

        mockMvc.perform(put("/api/dashboard/widgets/" + createdWidgetId)
                .header("Authorization", "Bearer " + testAccessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Updated Temperature Gauge"));
    }

    @Test
    @DisplayName("위젯 삭제 성공")
    void testDeleteWidgetSuccess() throws Exception {
        // 먼저 위젯 생성
        WidgetRequestDto request = WidgetRequestDto.builder()
                .equipmentId("CVD-CHAMBER-01")
                .widgetType("GAUGE")
                .title("Temperature Gauge")
                .posX(0)
                .posY(0)
                .width(2)
                .height(2)
                .build();

        String createResponse = mockMvc.perform(post("/api/dashboard/widgets")
                .header("Authorization", "Bearer " + testAccessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // 응답에서 widgetId 추출
        Long createdWidgetId = objectMapper.readTree(createResponse).path("data").path("id").asLong();

        // 위젯 삭제
        mockMvc.perform(delete("/api/dashboard/widgets/" + createdWidgetId)
                .header("Authorization", "Bearer " + testAccessToken)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("위젯 레이아웃 일괄 저장 성공")
    void testUpdateLayoutsSuccess() throws Exception {
        // 먼저 위젯 생성
        WidgetRequestDto createRequest = WidgetRequestDto.builder()
                .equipmentId("CVD-CHAMBER-01")
                .widgetType("GAUGE")
                .title("Temperature Gauge")
                .posX(0)
                .posY(0)
                .width(2)
                .height(2)
                .build();

        String createResponse = mockMvc.perform(post("/api/dashboard/widgets")
                .header("Authorization", "Bearer " + testAccessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // 응답에서 widgetId 추출
        Long createdWidgetId = objectMapper.readTree(createResponse).path("data").path("id").asLong();

        // 레이아웃 업데이트
        WidgetLayoutUpdateDto.LayoutItem layoutItem = WidgetLayoutUpdateDto.LayoutItem.builder()
                .widgetId(createdWidgetId)
                .posX(5)
                .posY(5)
                .width(3)
                .height(3)
                .build();

        WidgetLayoutUpdateDto layoutRequest = WidgetLayoutUpdateDto.builder()
                .layouts(Arrays.asList(layoutItem))
                .build();

        mockMvc.perform(put("/api/dashboard/widgets/layout")
                .header("Authorization", "Bearer " + testAccessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(layoutRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].posX").value(5))
                .andExpect(jsonPath("$.data[0].posY").value(5));
    }
}

