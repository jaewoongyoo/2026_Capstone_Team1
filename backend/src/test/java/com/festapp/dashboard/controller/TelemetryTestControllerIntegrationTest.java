package com.festapp.dashboard.controller;

import com.festapp.dashboard.common.security.JwtProvider;
import com.festapp.dashboard.config.IntegrationTestConfig;
import com.festapp.dashboard.dashboard.entity.Dashboard;
import com.festapp.dashboard.dashboard.repository.DashboardRepository;
import com.festapp.dashboard.equipment.entity.Equipment;
import com.festapp.dashboard.equipment.repository.EquipmentRepository;
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

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
@Import(IntegrationTestConfig.class)
@Transactional
@DisplayName("Telemetry 테스트 API 통합 테스트")
class TelemetryTestControllerIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DashboardRepository dashboardRepository;

    @Autowired
    private EquipmentRepository equipmentRepository;

    @Autowired
    private SensorRepository sensorRepository;

    @Autowired
    private SensorNumericHistoryRepository sensorNumericHistoryRepository;

    @Autowired
    private SensorStringHistoryRepository sensorStringHistoryRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtProvider jwtProvider;

    private MockMvc mockMvc;
    private String accessToken;
    private Equipment equipment;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        sensorStringHistoryRepository.deleteAll();
        sensorNumericHistoryRepository.deleteAll();
        sensorRepository.deleteAll();
        equipmentRepository.deleteAll();
        dashboardRepository.deleteAll();
        userRepository.deleteAll();

        User user = userRepository.save(User.builder()
                .username("telemetry-controller-user")
                .email("telemetry-controller@example.com")
                .password(passwordEncoder.encode("Password123!"))
                .fullName("Telemetry Controller User")
                .role(User.Role.USER)
                .isActive(true)
                .build());

        Dashboard dashboard = dashboardRepository.save(Dashboard.builder()
                .dashboardName("Telemetry Controller Dashboard")
                .description("Telemetry Controller Dashboard")
                .user(user)
                .build());

        equipment = equipmentRepository.save(Equipment.builder()
                .equipmentName("CVD-CTRL-01")
                .field("CVD")
                .dashboard(dashboard)
                .build());

        accessToken = jwtProvider.generateAccessTokenFromUserId(
                user.getUserId(),
                user.getUsername(),
                user.getRole().toString()
        );
    }

    @Test
    @DisplayName("유효한 equipmentEntityId telemetry는 201과 함께 이력을 저장한다")
    void validTelemetryReturnsCreatedAndPersistsHistory() throws Exception {
        String payload = """
                {
                  "equipmentEntityId": %d,
                  "equipmentId": "CVD-CTRL-01",
                  "timestamp": "2026-05-13T00:00:00Z",
                  "status": "RUN",
                  "sensors": [
                    {"sensorId": "Temp_0", "value": 971.5, "unit": "C"},
                    {"sensorId": "Valve_Status", "value": "OPEN", "unit": "STATUS"}
                  ]
                }
                """.formatted(equipment.getEquipmentId());

        mockMvc.perform(post("/api/telemetry/test")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.equipmentEntityId").value(equipment.getEquipmentId()))
                .andExpect(jsonPath("$.data.sensors", hasSize(2)));

        mockMvc.perform(post("/api/telemetry/test")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("equipmentEntityId와 equipmentId가 불일치하면 202로 skip을 알린다")
    void mismatchedEquipmentReferenceReturnsAccepted() throws Exception {
        String payload = """
                {
                  "equipmentEntityId": %d,
                  "equipmentId": "WRONG-EQUIPMENT-NAME",
                  "timestamp": "2026-05-13T00:00:00Z",
                  "status": "RUN",
                  "sensors": [
                    {"sensorId": "Temp_0", "value": 971.5, "unit": "C"}
                  ]
                }
                """.formatted(equipment.getEquipmentId());

        mockMvc.perform(post("/api/telemetry/test")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Telemetry 대상 장비가 없어 저장과 브로드캐스트를 건너뛰었습니다"));
    }

    @Test
    @DisplayName("장비 참조가 없는 telemetry는 400을 반환한다")
    void missingEquipmentReferenceReturnsBadRequest() throws Exception {
        String payload = """
                {
                  "sensors": [
                    {"sensorId": "Temp_0", "value": 971.5, "unit": "C"}
                  ]
                }
                """;

        mockMvc.perform(post("/api/telemetry/test")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value(4100))
                .andExpect(jsonPath("$.errorDetail").value("equipmentReference: equipmentEntityId 또는 equipmentId 중 하나는 필수입니다"));
    }

    @Test
    @DisplayName("인증 없이 telemetry test를 호출하면 401을 반환한다")
    void telemetryRequiresAuthentication() throws Exception {
        String payload = """
                {
                  "equipmentEntityId": %d,
                  "sensors": [
                    {"sensorId": "Temp_0", "value": 971.5, "unit": "C"}
                  ]
                }
                """.formatted(equipment.getEquipmentId());

        mockMvc.perform(post("/api/telemetry/test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized());
    }
}
