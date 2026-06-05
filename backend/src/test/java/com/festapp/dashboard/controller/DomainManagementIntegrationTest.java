package com.festapp.dashboard.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.festapp.dashboard.common.security.JwtProvider;
import com.festapp.dashboard.config.IntegrationTestConfig;
import com.festapp.dashboard.telemetry.entity.Sensor;
import com.festapp.dashboard.telemetry.entity.SensorNumericHistory;
import com.festapp.dashboard.telemetry.entity.SensorStringHistory;
import com.festapp.dashboard.telemetry.repository.SensorNumericHistoryRepository;
import com.festapp.dashboard.telemetry.repository.SensorRepository;
import com.festapp.dashboard.telemetry.repository.SensorStringHistoryRepository;
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

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
@Import(IntegrationTestConfig.class)
@Transactional
@DisplayName("도메인 관리 API 통합 테스트")
class DomainManagementIntegrationTest {

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

    @Autowired
    private SensorRepository sensorRepository;

    @Autowired
    private SensorNumericHistoryRepository sensorNumericHistoryRepository;

    @Autowired
    private SensorStringHistoryRepository sensorStringHistoryRepository;

    private MockMvc mockMvc;
    private String accessToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        userRepository.deleteAll();

        User user = userRepository.save(User.builder()
                .username("domainuser")
                .email("domain@example.com")
                .password(passwordEncoder.encode("Password123!"))
                .fullName("Domain User")
                .role(User.Role.USER)
                .isActive(true)
                .build());

        accessToken = jwtProvider.generateAccessTokenFromUserId(
                user.getUserId(),
                user.getUsername(),
                user.getRole().toString()
        );
    }

    @Test
    @DisplayName("대시보드 CRUD 성공")
    void dashboardCrudSuccess() throws Exception {
        String createPayload = """
                {
                  "dashboardName": "Fab Dashboard",
                  "description": "Main fab monitoring board"
                }
                """;

        String createResponse = mockMvc.perform(post("/api/dashboards")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.dashboardName").value("Fab Dashboard"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long dashboardId = readId(createResponse, "dashboardId");

        mockMvc.perform(get("/api/dashboards")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)));

        String updatePayload = """
                {
                  "dashboardName": "Fab Dashboard Updated",
                  "description": "Updated description"
                }
                """;

        mockMvc.perform(put("/api/dashboards/{dashboardId}", dashboardId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.dashboardName").value("Fab Dashboard Updated"));

        mockMvc.perform(delete("/api/dashboards/{dashboardId}", dashboardId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("장비 및 센서 CRUD 성공")
    void equipmentAndSensorCrudSuccess() throws Exception {
        Long dashboardId = createDashboard("Line Dashboard");

        String equipmentPayload = """
                {
                  "dashboardId": %d,
                  "equipmentName": "CVD-CHAMBER-01",
                  "field": "CVD"
                }
                """.formatted(dashboardId);

        String equipmentResponse = mockMvc.perform(post("/api/equipment")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(equipmentPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.equipmentName").value("CVD-CHAMBER-01"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long equipmentId = readId(equipmentResponse, "equipmentId");

        mockMvc.perform(get("/api/equipment/dashboard/{dashboardId}", dashboardId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)));

        String sensorPayload = """
                {
                  "equipmentId": %d,
                  "sensorName": "Temp_Sensor_001"
                }
                """.formatted(equipmentId);

        String sensorResponse = mockMvc.perform(post("/api/sensors")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sensorPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.sensorName").value("Temp_Sensor_001"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long sensorId = readId(sensorResponse, "sensorId");

        mockMvc.perform(get("/api/sensors/equipment/{equipmentId}", equipmentId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)));

        String updateSensorPayload = """
                {
                  "equipmentId": %d,
                  "sensorName": "Temp_Sensor_002"
                }
                """.formatted(equipmentId);

        mockMvc.perform(put("/api/sensors/{sensorId}", sensorId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateSensorPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sensorName").value("Temp_Sensor_002"));

        mockMvc.perform(delete("/api/sensors/{sensorId}", sensorId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/equipment/{equipmentId}", equipmentId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("센서 삭제 시 위젯 참조가 정리되고 장비 제거 시 위젯만 삭제된다")
    void deletingSensorClearsWidgetReferenceAndRemovingEquipmentDeletesWidgetOnly() throws Exception {
        Long dashboardId = createDashboard("Widget Cleanup Dashboard");
        Long equipmentId = createEquipment(dashboardId, "ETCH-02", "ETCH");
        Long sensorId = createSensor(equipmentId, "Pressure_Sensor_001");
        Long widgetId = createWidget(dashboardId, "ETCH-02", "Pressure_Sensor_001");

        mockMvc.perform(delete("/api/sensors/{sensorId}", sensorId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/dashboard/widgets/{id}", widgetId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sensorId").value(nullValue()))
                .andExpect(jsonPath("$.data.equipmentId").value("ETCH-02"));

        mockMvc.perform(delete("/api/equipment/{equipmentId}", equipmentId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/dashboard/widgets/{id}", widgetId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/equipment/{equipmentId}", equipmentId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.equipmentId").value(equipmentId))
                .andExpect(jsonPath("$.data.equipmentName").value("ETCH-02"));
    }

    @Test
    @DisplayName("센서 삭제 시 이력이 삭제되고 장비 제거 시 센서와 이력은 보존된다")
    void deletingSensorRemovesHistoriesAndRemovingEquipmentPreservesSensorHistories() throws Exception {
        Long dashboardId = createDashboard("History Cleanup Dashboard");
        Long equipmentId = createEquipment(dashboardId, "ETCH-03", "ETCH");
        Long sensorId = createSensor(equipmentId, "Flow_Sensor_001");

        Sensor sensor = sensorRepository.findById(sensorId).orElseThrow();
        sensorNumericHistoryRepository.save(SensorNumericHistory.builder()
                .sensor(sensor)
                .value(101.5)
                .dataType("FLOAT")
                .unit("L/MIN")
                .timestamp(LocalDateTime.of(2026, 4, 17, 12, 0))
                .build());
        sensorStringHistoryRepository.save(SensorStringHistory.builder()
                .sensor(sensor)
                .status("NORMAL")
                .timestamp(LocalDateTime.of(2026, 4, 17, 12, 1))
                .build());

        mockMvc.perform(delete("/api/sensors/{sensorId}", sensorId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/sensors/{sensorId}/history/numeric", sensorId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/sensors/{sensorId}/history/string", sensorId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound());

        Long equipmentId2 = createEquipment(dashboardId, "ETCH-04", "ETCH");
        Long sensorId2 = createSensor(equipmentId2, "Pressure_Sensor_002");

        Sensor sensor2 = sensorRepository.findById(sensorId2).orElseThrow();
        sensorNumericHistoryRepository.save(SensorNumericHistory.builder()
                .sensor(sensor2)
                .value(88.8)
                .dataType("FLOAT")
                .unit("Pa")
                .timestamp(LocalDateTime.of(2026, 4, 17, 12, 5))
                .build());

        mockMvc.perform(delete("/api/equipment/{equipmentId}", equipmentId2)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/equipment/{equipmentId}", equipmentId2)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.equipmentId").value(equipmentId2));

        mockMvc.perform(get("/api/sensors/{sensorId}", sensorId2)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sensorId").value(sensorId2));

        mockMvc.perform(get("/api/sensors/{sensorId}/history/numeric", sensorId2)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].value").value(88.8));
    }

    @Test
    @DisplayName("대시보드/장비 PK 기반 위젯 API 사용 가능")
    void widgetApiSupportsEntityIds() throws Exception {
        Long dashboardId = createDashboard("Entity Widget Dashboard");
        Long equipmentId = createEquipment(dashboardId, "CVD-CHAMBER-09", "CVD");
        Long sensorId = createSensor(equipmentId, "Temp_Sensor_009");

        String widgetPayload = """
                {
                  "equipmentEntityId": %d,
                  "sensorEntityId": %d,
                  "widgetType": "GAUGE",
                  "title": "Entity Id Widget",
                  "chartType": "line",
                  "dataType": "FLOAT",
                  "unit": "C",
                  "posX": 1,
                  "posY": 2,
                  "width": 3,
                  "height": 2
                }
                """.formatted(equipmentId, sensorId);

        mockMvc.perform(post("/api/dashboards/{dashboardId}/widgets", dashboardId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(widgetPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.dashboardId").value(dashboardId))
                .andExpect(jsonPath("$.data.equipmentEntityId").value(equipmentId))
                .andExpect(jsonPath("$.data.sensorEntityId").value(sensorId))
                .andExpect(jsonPath("$.data.equipmentId").value("CVD-CHAMBER-09"))
                .andExpect(jsonPath("$.data.sensorId").value("Temp_Sensor_009"));

        mockMvc.perform(get("/api/dashboards/{dashboardId}/widgets", dashboardId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)));

        mockMvc.perform(get("/api/equipment/{equipmentId}/widgets", equipmentId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].equipmentEntityId").value(equipmentId));
    }

    @Test
    @DisplayName("타 사용자 리소스 접근 차단")
    void forbidOtherUsersResourceAccess() throws Exception {
        Long dashboardId = createDashboard("Private Dashboard");

        User otherUser = userRepository.save(User.builder()
                .username("otherdomain")
                .email("otherdomain@example.com")
                .password(passwordEncoder.encode("Password123!"))
                .fullName("Other User")
                .role(User.Role.USER)
                .isActive(true)
                .build());

        String otherToken = jwtProvider.generateAccessTokenFromUserId(
                otherUser.getUserId(),
                otherUser.getUsername(),
                otherUser.getRole().toString()
        );

        mockMvc.perform(get("/api/dashboards/{dashboardId}", dashboardId)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value(4205));
    }

    private Long createDashboard(String dashboardName) throws Exception {
        String payload = """
                {
                  "dashboardName": "%s",
                  "description": "Created from test"
                }
                """.formatted(dashboardName);

        String response = mockMvc.perform(post("/api/dashboards")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return readId(response, "dashboardId");
    }

    private Long createEquipment(Long dashboardId, String equipmentName, String field) throws Exception {
        String equipmentPayload = """
                {
                  "dashboardId": %d,
                  "equipmentName": "%s",
                  "field": "%s"
                }
                """.formatted(dashboardId, equipmentName, field);

        String response = mockMvc.perform(post("/api/equipment")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(equipmentPayload))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return readId(response, "equipmentId");
    }

    private Long createSensor(Long equipmentId, String sensorName) throws Exception {
        String sensorPayload = """
                {
                  "equipmentId": %d,
                  "sensorName": "%s"
                }
                """.formatted(equipmentId, sensorName);

        String response = mockMvc.perform(post("/api/sensors")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sensorPayload))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return readId(response, "sensorId");
    }

    private Long createWidget(Long dashboardId, String equipmentId, String sensorId) throws Exception {
        String widgetPayload = """
                {
                  "dashboardId": %d,
                  "equipmentId": "%s",
                  "widgetType": "GAUGE",
                  "title": "Pressure Widget",
                  "sensorId": "%s",
                  "chartType": "line",
                  "dataType": "FLOAT",
                  "unit": "Pa",
                  "posX": 0,
                  "posY": 0,
                  "width": 2,
                  "height": 2
                }
                """.formatted(dashboardId, equipmentId, sensorId);

        String response = mockMvc.perform(post("/api/dashboard/widgets")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(widgetPayload))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return readId(response, "id");
    }

    private Long readId(String response, String fieldName) throws Exception {
        JsonNode root = objectMapper.readTree(response);
        return root.path("data").path(fieldName).asLong();
    }
}
