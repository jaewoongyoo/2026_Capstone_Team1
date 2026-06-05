// 대시보드 위젯 API 향상된 통합 테스트 - 예외처리 및 에러 케이스 검증
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
import org.junit.jupiter.api.Nested;
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

/**
 * 대시보드 위젯 API 향상된 통합 테스트
 * 
 * 프론트엔드 개발자 관점에서 예외처리, 에러 응답, 입력 검증을 종합적으로 테스트합니다.
 * 
 * @author DashBoar Team
 * @version 1.0.0
 */
@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
@Import(IntegrationTestConfig.class)
@DisplayName("DashboardWidget API 향상된 통합 테스트")
@Transactional
public class DashboardWidgetAPIEnhancedTest {

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
    private User testUser1;
    private User testUser2;
    private String testAccessToken1;
    private String testAccessToken2;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        userRepository.deleteAll();

        // 테스트 사용자 1
        testUser1 = User.builder()
                .username("widget_user1")
                .email("widget1@example.com")
                .password(passwordEncoder.encode("WidgetPass123!"))
                .fullName("Widget User 1")
                .role(User.Role.USER)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        testUser1 = userRepository.save(testUser1);

        testAccessToken1 = jwtProvider.generateAccessTokenFromUserId(
                testUser1.getUserId(),
                testUser1.getUsername(),
                testUser1.getRole().toString()
        );

        // 테스트 사용자 2
        testUser2 = User.builder()
                .username("widget_user2")
                .email("widget2@example.com")
                .password(passwordEncoder.encode("WidgetPass123!"))
                .fullName("Widget User 2")
                .role(User.Role.USER)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        testUser2 = userRepository.save(testUser2);

        testAccessToken2 = jwtProvider.generateAccessTokenFromUserId(
                testUser2.getUserId(),
                testUser2.getUsername(),
                testUser2.getRole().toString()
        );
    }

    @Nested
    @DisplayName("입력 검증 테스트")
    class InputValidationTest {

        @Test
        @DisplayName("필수 필드(equipmentId) 누락 시 400 Bad Request")
        void testCreateWidgetMissingEquipmentId() throws Exception {
            WidgetRequestDto request = WidgetRequestDto.builder()
                    // equipmentId 누락
                    .widgetType("GAUGE")
                    .title("Temperature Gauge")
                    .posX(0)
                    .posY(0)
                    .width(4)
                    .height(3)
                    .build();

            mockMvc.perform(post("/api/dashboard/widgets")
                    .header("Authorization", "Bearer " + testAccessToken1)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success", is(false)))
                    .andExpect(jsonPath("$.errorCode", is(4100)))
                    .andExpect(jsonPath("$.message", containsString("검증")))
                    .andExpect(jsonPath("$.errorDetail", notNullValue()));
        }

        @Test
        @DisplayName("필드 길이 초과 시 400 Bad Request")
        void testCreateWidgetFieldLengthExceeded() throws Exception {
            String longTitle = "A".repeat(256); // 255자 초과
            WidgetRequestDto request = WidgetRequestDto.builder()
                    .equipmentId("CHAMBER-01")
                    .widgetType("GAUGE")
                    .title(longTitle)
                    .posX(0)
                    .posY(0)
                    .width(4)
                    .height(3)
                    .build();

            mockMvc.perform(post("/api/dashboard/widgets")
                    .header("Authorization", "Bearer " + testAccessToken1)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success", is(false)))
                    .andExpect(jsonPath("$.errorCode", is(4100)));
        }

        @Test
        @DisplayName("잘못된 숫자 범위(width < 1) 시 400 Bad Request")
        void testCreateWidgetInvalidWidth() throws Exception {
            WidgetRequestDto request = WidgetRequestDto.builder()
                    .equipmentId("CHAMBER-01")
                    .widgetType("GAUGE")
                    .title("Temperature Gauge")
                    .posX(0)
                    .posY(0)
                    .width(0) // 1 미만
                    .height(3)
                    .build();

            mockMvc.perform(post("/api/dashboard/widgets")
                    .header("Authorization", "Bearer " + testAccessToken1)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode", is(4100)));
        }

        @Test
        @DisplayName("레이아웃 업데이트 시 필수 필드(widgetId) 누락 시 400 Bad Request")
        void testUpdateLayoutMissingWidgetId() throws Exception {
            // JSON으로 직접 보내서 widgetId를 누락
            // @Valid 검증에서 widgetId 필수 조건이 먼저 실패하므로 400 반환
            String requestJson = "{\n" +
                    "  \"layouts\": [\n" +
                    "    {\n" +
                    "      \"posX\": 2,\n" +
                    "      \"posY\": 1,\n" +
                    "      \"width\": 4,\n" +
                    "      \"height\": 3\n" +
                    "    }\n" +
                    "  ]\n" +
                    "}";

            mockMvc.perform(put("/api/dashboard/widgets/layout")
                    .header("Authorization", "Bearer " + testAccessToken1)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode", is(4100)));
        }
    }

    @Nested
    @DisplayName("인증 테스트")
    class AuthenticationTest {

        @Test
        @DisplayName("인증 없이 요청 시 401 Unauthorized")
        void testCreateWidgetWithoutAuth() throws Exception {
            WidgetRequestDto request = WidgetRequestDto.builder()
                    .equipmentId("CHAMBER-01")
                    .widgetType("GAUGE")
                    .title("Temperature Gauge")
                    .posX(0)
                    .posY(0)
                    .width(4)
                    .height(3)
                    .build();

            mockMvc.perform(post("/api/dashboard/widgets")
                    // Authorization 헤더 생략
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.errorCode", is(4007)))
                    .andExpect(jsonPath("$.statusCode", is(401)));
        }

        @Test
        @DisplayName("잘못된 토큰으로 요청 시 401 Unauthorized")
        void testCreateWidgetWithInvalidToken() throws Exception {
            WidgetRequestDto request = WidgetRequestDto.builder()
                    .equipmentId("CHAMBER-01")
                    .widgetType("GAUGE")
                    .title("Temperature Gauge")
                    .posX(0)
                    .posY(0)
                    .width(4)
                    .height(3)
                    .build();

            mockMvc.perform(post("/api/dashboard/widgets")
                    .header("Authorization", "Bearer invalid.token.here")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("권한 및 소유권 테스트")
    class OwnershipAndAuthorizationTest {

        @Test
        @DisplayName("다른 사용자의 위젯 조회 시 404 Not Found")
        void testGetOtherUserWidgetNotFound() throws Exception {
            // 사용자1이 위젯 생성
            WidgetRequestDto createRequest = WidgetRequestDto.builder()
                    .equipmentId("CHAMBER-01")
                    .widgetType("GAUGE")
                    .title("Temperature Gauge")
                    .posX(0)
                    .posY(0)
                    .width(4)
                    .height(3)
                    .build();

            String createResponse = mockMvc.perform(post("/api/dashboard/widgets")
                    .header("Authorization", "Bearer " + testAccessToken1)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createRequest)))
                    .andExpect(status().isCreated())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            Long createdWidgetId = objectMapper.readTree(createResponse)
                    .path("data").path("id").asLong();

            // 사용자2가 사용자1의 위젯 조회 시도
            mockMvc.perform(get("/api/dashboard/widgets/" + createdWidgetId)
                    .header("Authorization", "Bearer " + testAccessToken2))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorCode", is(4201)))
                    .andExpect(jsonPath("$.message", containsString("위젯")));
        }

        @Test
        @DisplayName("다른 사용자의 위젯 수정 시 404 Not Found")
        void testUpdateOtherUserWidgetNotFound() throws Exception {
            // 사용자1이 위젯 생성
            WidgetRequestDto createRequest = WidgetRequestDto.builder()
                    .equipmentId("CHAMBER-01")
                    .widgetType("GAUGE")
                    .title("Temperature Gauge")
                    .posX(0)
                    .posY(0)
                    .width(4)
                    .height(3)
                    .build();

            String createResponse = mockMvc.perform(post("/api/dashboard/widgets")
                    .header("Authorization", "Bearer " + testAccessToken1)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createRequest)))
                    .andExpect(status().isCreated())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            Long createdWidgetId = objectMapper.readTree(createResponse)
                    .path("data").path("id").asLong();

            // 사용자2가 사용자1의 위젯 수정 시도
            WidgetRequestDto updateRequest = WidgetRequestDto.builder()
                    .equipmentId("CHAMBER-02")
                    .widgetType("GAUGE")
                    .title("Updated Title")
                    .posX(1)
                    .posY(1)
                    .width(4)
                    .height(3)
                    .build();

            mockMvc.perform(put("/api/dashboard/widgets/" + createdWidgetId)
                    .header("Authorization", "Bearer " + testAccessToken2)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorCode", is(4201)));
        }

        @Test
        @DisplayName("다른 사용자의 위젯 삭제 시 404 Not Found")
        void testDeleteOtherUserWidgetNotFound() throws Exception {
            // 사용자1이 위젯 생성
            WidgetRequestDto createRequest = WidgetRequestDto.builder()
                    .equipmentId("CHAMBER-01")
                    .widgetType("GAUGE")
                    .title("Temperature Gauge")
                    .posX(0)
                    .posY(0)
                    .width(4)
                    .height(3)
                    .build();

            String createResponse = mockMvc.perform(post("/api/dashboard/widgets")
                    .header("Authorization", "Bearer " + testAccessToken1)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createRequest)))
                    .andExpect(status().isCreated())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            Long createdWidgetId = objectMapper.readTree(createResponse)
                    .path("data").path("id").asLong();

            // 사용자2가 사용자1의 위젯 삭제 시도
            mockMvc.perform(delete("/api/dashboard/widgets/" + createdWidgetId)
                    .header("Authorization", "Bearer " + testAccessToken2))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorCode", is(4201)));
        }

        @Test
        @DisplayName("존재하지 않는 위젯 조회 시 404 Not Found")
        void testGetNonExistentWidget() throws Exception {
            mockMvc.perform(get("/api/dashboard/widgets/99999")
                    .header("Authorization", "Bearer " + testAccessToken1))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorCode", is(4201)));
        }

        @Test
        @DisplayName("존재하지 않는 위젯 수정 시 404 Not Found")
        void testUpdateNonExistentWidget() throws Exception {
            WidgetRequestDto updateRequest = WidgetRequestDto.builder()
                    .equipmentId("CHAMBER-02")
                    .widgetType("GAUGE")
                    .title("Updated Title")
                    .posX(1)
                    .posY(1)
                    .width(4)
                    .height(3)
                    .build();

            mockMvc.perform(put("/api/dashboard/widgets/99999")
                    .header("Authorization", "Bearer " + testAccessToken1)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorCode", is(4201)));
        }

        @Test
        @DisplayName("존재하지 않는 위젯 삭제 시 404 Not Found")
        void testDeleteNonExistentWidget() throws Exception {
            mockMvc.perform(delete("/api/dashboard/widgets/99999")
                    .header("Authorization", "Bearer " + testAccessToken1))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorCode", is(4201)));
        }

        @Test
        @DisplayName("레이아웃 업데이트 중 하나의 위젯이 없으면 404 Not Found")
        void testUpdateLayoutWithNonExistentWidget() throws Exception {
            // 사용자1이 위젯 생성
            WidgetRequestDto createRequest = WidgetRequestDto.builder()
                    .equipmentId("CHAMBER-01")
                    .widgetType("GAUGE")
                    .title("Temperature Gauge")
                    .posX(0)
                    .posY(0)
                    .width(4)
                    .height(3)
                    .build();

            String createResponse = mockMvc.perform(post("/api/dashboard/widgets")
                    .header("Authorization", "Bearer " + testAccessToken1)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createRequest)))
                    .andExpect(status().isCreated())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            Long createdWidgetId = objectMapper.readTree(createResponse)
                    .path("data").path("id").asLong();

            // 존재하는 위젯과 존재하지 않는 위젯을 동시에 업데이트 시도
            WidgetLayoutUpdateDto.LayoutItem item1 = WidgetLayoutUpdateDto.LayoutItem.builder()
                    .widgetId(createdWidgetId)
                    .posX(2)
                    .posY(1)
                    .width(4)
                    .height(3)
                    .build();

            WidgetLayoutUpdateDto.LayoutItem item2 = WidgetLayoutUpdateDto.LayoutItem.builder()
                    .widgetId(99999L) // 존재하지 않는 위젯
                    .posX(0)
                    .posY(4)
                    .width(2)
                    .height(2)
                    .build();

            WidgetLayoutUpdateDto request = WidgetLayoutUpdateDto.builder()
                    .layouts(java.util.List.of(item1, item2))
                    .build();

            mockMvc.perform(put("/api/dashboard/widgets/layout")
                    .header("Authorization", "Bearer " + testAccessToken1)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorCode", is(4201)));
        }
    }

    @Nested
    @DisplayName("성공 케이스 테스트")
    class SuccessTest {

        @Test
        @DisplayName("위젯 생성 성공 시 201 Created와 위젯 정보 반환")
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
                    .width(4)
                    .height(3)
                    .configJson("{\"refreshInterval\":5000}")
                    .build();

            mockMvc.perform(post("/api/dashboard/widgets")
                    .header("Authorization", "Bearer " + testAccessToken1)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data.id", notNullValue()))
                    .andExpect(jsonPath("$.data.equipmentId", is("CVD-CHAMBER-01")))
                    .andExpect(jsonPath("$.data.title", is("Temperature Gauge")))
                    .andExpect(jsonPath("$.data.userId", is(testUser1.getUserId().intValue())))
                    .andExpect(jsonPath("$.data.createdAt", notNullValue()))
                    .andExpect(jsonPath("$.statusCode", is(201)));
        }

        @Test
        @DisplayName("위젯 조회 성공 시 200 OK와 위젯 정보 반환")
        void testGetWidgetSuccess() throws Exception {
            // 위젯 생성
            WidgetRequestDto createRequest = WidgetRequestDto.builder()
                    .equipmentId("CHAMBER-01")
                    .widgetType("GAUGE")
                    .title("Temperature Gauge")
                    .posX(0)
                    .posY(0)
                    .width(4)
                    .height(3)
                    .build();

            String createResponse = mockMvc.perform(post("/api/dashboard/widgets")
                    .header("Authorization", "Bearer " + testAccessToken1)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createRequest)))
                    .andExpect(status().isCreated())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            Long createdWidgetId = objectMapper.readTree(createResponse)
                    .path("data").path("id").asLong();

            // 위젯 조회
            mockMvc.perform(get("/api/dashboard/widgets/" + createdWidgetId)
                    .header("Authorization", "Bearer " + testAccessToken1))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data.id", is(createdWidgetId.intValue())))
                    .andExpect(jsonPath("$.data.title", is("Temperature Gauge")))
                    .andExpect(jsonPath("$.statusCode", is(200)));
        }

        @Test
        @DisplayName("위젯 수정 성공 시 200 OK와 수정된 정보 반환")
        void testUpdateWidgetSuccess() throws Exception {
            // 위젯 생성
            WidgetRequestDto createRequest = WidgetRequestDto.builder()
                    .equipmentId("CHAMBER-01")
                    .widgetType("GAUGE")
                    .title("Original Title")
                    .posX(0)
                    .posY(0)
                    .width(4)
                    .height(3)
                    .build();

            String createResponse = mockMvc.perform(post("/api/dashboard/widgets")
                    .header("Authorization", "Bearer " + testAccessToken1)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createRequest)))
                    .andExpect(status().isCreated())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            Long createdWidgetId = objectMapper.readTree(createResponse)
                    .path("data").path("id").asLong();

            // 위젯 수정
            WidgetRequestDto updateRequest = WidgetRequestDto.builder()
                    .equipmentId("CHAMBER-02")
                    .widgetType("GAUGE")
                    .title("Updated Title")
                    .posX(1)
                    .posY(1)
                    .width(5)
                    .height(4)
                    .build();

            mockMvc.perform(put("/api/dashboard/widgets/" + createdWidgetId)
                    .header("Authorization", "Bearer " + testAccessToken1)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data.title", is("Updated Title")))
                    .andExpect(jsonPath("$.data.equipmentId", is("CHAMBER-02")))
                    .andExpect(jsonPath("$.data.posX", is(1)))
                    .andExpect(jsonPath("$.data.updatedAt", notNullValue()));
        }

        @Test
        @DisplayName("위젯 삭제 성공 시 204 No Content")
        void testDeleteWidgetSuccess() throws Exception {
            // 위젯 생성
            WidgetRequestDto createRequest = WidgetRequestDto.builder()
                    .equipmentId("CHAMBER-01")
                    .widgetType("GAUGE")
                    .title("Temperature Gauge")
                    .posX(0)
                    .posY(0)
                    .width(4)
                    .height(3)
                    .build();

            String createResponse = mockMvc.perform(post("/api/dashboard/widgets")
                    .header("Authorization", "Bearer " + testAccessToken1)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createRequest)))
                    .andExpect(status().isCreated())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            Long createdWidgetId = objectMapper.readTree(createResponse)
                    .path("data").path("id").asLong();

            // 위젯 삭제
            mockMvc.perform(delete("/api/dashboard/widgets/" + createdWidgetId)
                    .header("Authorization", "Bearer " + testAccessToken1))
                    .andExpect(status().isNoContent());

            // 삭제 후 조회 시 404
            mockMvc.perform(get("/api/dashboard/widgets/" + createdWidgetId)
                    .header("Authorization", "Bearer " + testAccessToken1))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("레이아웃 일괄 업데이트 성공 시 200 OK와 모든 위젯 정보 반환")
        void testUpdateLayoutSuccess() throws Exception {
            // 2개 위젯 생성
            WidgetRequestDto createRequest1 = WidgetRequestDto.builder()
                    .equipmentId("CHAMBER-01")
                    .widgetType("GAUGE")
                    .title("Widget 1")
                    .posX(0)
                    .posY(0)
                    .width(4)
                    .height(3)
                    .build();

            String createResponse1 = mockMvc.perform(post("/api/dashboard/widgets")
                    .header("Authorization", "Bearer " + testAccessToken1)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createRequest1)))
                    .andExpect(status().isCreated())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            Long widgetId1 = objectMapper.readTree(createResponse1)
                    .path("data").path("id").asLong();

            WidgetRequestDto createRequest2 = WidgetRequestDto.builder()
                    .equipmentId("CHAMBER-02")
                    .widgetType("CHART")
                    .title("Widget 2")
                    .posX(4)
                    .posY(0)
                    .width(4)
                    .height(3)
                    .build();

            String createResponse2 = mockMvc.perform(post("/api/dashboard/widgets")
                    .header("Authorization", "Bearer " + testAccessToken1)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createRequest2)))
                    .andExpect(status().isCreated())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            Long widgetId2 = objectMapper.readTree(createResponse2)
                    .path("data").path("id").asLong();

            // 레이아웃 업데이트
            WidgetLayoutUpdateDto.LayoutItem item1 = WidgetLayoutUpdateDto.LayoutItem.builder()
                    .widgetId(widgetId1)
                    .posX(2)
                    .posY(1)
                    .width(5)
                    .height(4)
                    .build();

            WidgetLayoutUpdateDto.LayoutItem item2 = WidgetLayoutUpdateDto.LayoutItem.builder()
                    .widgetId(widgetId2)
                    .posX(8)
                    .posY(2)
                    .width(3)
                    .height(2)
                    .build();

            WidgetLayoutUpdateDto request = WidgetLayoutUpdateDto.builder()
                    .layouts(java.util.List.of(item1, item2))
                    .build();

            mockMvc.perform(put("/api/dashboard/widgets/layout")
                    .header("Authorization", "Bearer " + testAccessToken1)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data", hasSize(2)))
                    .andExpect(jsonPath("$.data[0].posX", is(2)))
                    .andExpect(jsonPath("$.data[1].posX", is(8)));
        }
    }

    @Nested
    @DisplayName("에러 응답 형식 테스트")
    class ErrorResponseFormatTest {

        @Test
        @DisplayName("에러 응답에 필수 필드 포함 확인")
        void testErrorResponseHasRequiredFields() throws Exception {
            // 필수 필드 누락 요청
            WidgetRequestDto request = WidgetRequestDto.builder()
                    .widgetType("GAUGE")
                    .title("Title")
                    .posX(0)
                    .posY(0)
                    .width(4)
                    .height(3)
                    .build();

            mockMvc.perform(post("/api/dashboard/widgets")
                    .header("Authorization", "Bearer " + testAccessToken1)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success", is(false)))
                    .andExpect(jsonPath("$.message", notNullValue()))
                    .andExpect(jsonPath("$.errorCode", notNullValue()))
                    .andExpect(jsonPath("$.statusCode", is(400)))
                    .andExpect(jsonPath("$.timestamp", notNullValue()))
                    .andExpect(jsonPath("$.path", notNullValue()));
        }

        @Test
        @DisplayName("404 Not Found 에러 코드 확인")
        void testNotFoundErrorCode() throws Exception {
            mockMvc.perform(get("/api/dashboard/widgets/99999")
                    .header("Authorization", "Bearer " + testAccessToken1))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorCode", is(4201))) // WIDGET_NOT_FOUND
                    .andExpect(jsonPath("$.statusCode", is(404)));
        }

        @Test
        @DisplayName("401 Unauthorized 에러 코드 확인")
        void testUnauthorizedErrorCode() throws Exception {
            WidgetRequestDto request = WidgetRequestDto.builder()
                    .equipmentId("CHAMBER-01")
                    .widgetType("GAUGE")
                    .title("Title")
                    .posX(0)
                    .posY(0)
                    .width(4)
                    .height(3)
                    .build();

            mockMvc.perform(post("/api/dashboard/widgets")
                    // 인증 헤더 생략
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.errorCode", is(4007))) // AUTHENTICATION_REQUIRED
                    .andExpect(jsonPath("$.statusCode", is(401)));
        }
    }
}

