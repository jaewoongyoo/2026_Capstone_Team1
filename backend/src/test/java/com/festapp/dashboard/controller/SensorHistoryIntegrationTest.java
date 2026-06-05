package com.festapp.dashboard.controller;

import com.festapp.dashboard.common.security.JwtProvider;
import com.festapp.dashboard.dashboard.entity.Dashboard;
import com.festapp.dashboard.dashboard.repository.DashboardRepository;
import com.festapp.dashboard.dashboard.widget.repository.DashboardWidgetRepository;
import com.festapp.dashboard.equipment.entity.Equipment;
import com.festapp.dashboard.equipment.repository.EquipmentRepository;
import com.festapp.dashboard.telemetry.entity.Sensor;
import com.festapp.dashboard.telemetry.entity.SensorNumericHistory;
import com.festapp.dashboard.telemetry.entity.SensorStringHistory;
import com.festapp.dashboard.telemetry.repository.SensorNumericHistoryRepository;
import com.festapp.dashboard.telemetry.repository.SensorRepository;
import com.festapp.dashboard.telemetry.repository.SensorStringHistoryRepository;
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

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
@Import(IntegrationTestConfig.class)
@Transactional
@DisplayName("센서 이력 API 통합 테스트")
class SensorHistoryIntegrationTest {

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
    private DashboardWidgetRepository dashboardWidgetRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtProvider jwtProvider;

    private MockMvc mockMvc;
    private String accessToken;
    private Long sensorId;
    private Long otherUserSensorId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        dashboardWidgetRepository.deleteAll();
        sensorStringHistoryRepository.deleteAll();
        sensorNumericHistoryRepository.deleteAll();
        sensorRepository.deleteAll();
        equipmentRepository.deleteAll();
        dashboardRepository.deleteAll();
        userRepository.deleteAll();

        User user = userRepository.save(User.builder()
                .username("history-user")
                .email("history@example.com")
                .password(passwordEncoder.encode("Password123!"))
                .fullName("History User")
                .role(User.Role.USER)
                .isActive(true)
                .build());

        Dashboard dashboard = dashboardRepository.save(Dashboard.builder()
                .dashboardName("History Dashboard")
                .description("history")
                .user(user)
                .build());

        Equipment equipment = equipmentRepository.save(Equipment.builder()
                .equipmentName("ETCH-01")
                .field("ETCH")
                .dashboard(dashboard)
                .build());

        Sensor sensor = sensorRepository.save(Sensor.builder()
                .sensorName("Temp_Sensor_001")
                .equipment(equipment)
                .build());

        sensorId = sensor.getSensorId();

        User otherUser = userRepository.save(User.builder()
                .username("other-history-user")
                .email("other-history@example.com")
                .password(passwordEncoder.encode("Password123!"))
                .fullName("Other History User")
                .role(User.Role.USER)
                .isActive(true)
                .build());

        Dashboard otherDashboard = dashboardRepository.save(Dashboard.builder()
                .dashboardName("Other Dashboard")
                .description("other")
                .user(otherUser)
                .build());

        Equipment otherEquipment = equipmentRepository.save(Equipment.builder()
                .equipmentName("ETCH-02")
                .field("ETCH")
                .dashboard(otherDashboard)
                .build());

        Sensor otherSensor = sensorRepository.save(Sensor.builder()
                .sensorName("Temp_Sensor_999")
                .equipment(otherEquipment)
                .build());

        otherUserSensorId = otherSensor.getSensorId();

        sensorNumericHistoryRepository.save(SensorNumericHistory.builder()
                .sensor(sensor)
                .value(25.2)
                .dataType("FLOAT")
                .unit("C")
                .timestamp(LocalDateTime.of(2026, 4, 17, 12, 0))
                .build());

        sensorStringHistoryRepository.save(SensorStringHistory.builder()
                .sensor(sensor)
                .status("OPEN")
                .timestamp(LocalDateTime.of(2026, 4, 17, 12, 5))
                .build());

        accessToken = jwtProvider.generateAccessTokenFromUserId(user.getUserId(), user.getUsername(), user.getRole().toString());
    }

    @Test
    @DisplayName("숫자형 이력 조회 성공")
    void getNumericHistorySuccess() throws Exception {
        mockMvc.perform(get("/api/sensors/{sensorId}/history/numeric", sensorId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].value").value(25.2));
    }

    @Test
    @DisplayName("문자형 이력 기간 조회 성공")
    void getStringHistoryWithRangeSuccess() throws Exception {
        mockMvc.perform(get("/api/sensors/{sensorId}/history/string", sensorId)
                        .header("Authorization", "Bearer " + accessToken)
                        .param("from", "2026-04-17T12:00:00")
                        .param("to", "2026-04-17T12:10:00")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].status").value("OPEN"));
    }

    @Test
    @DisplayName("숫자형 이력 입력 성공")
    void createNumericHistorySuccess() throws Exception {
        mockMvc.perform(post("/api/sensors/{sensorId}/history/numeric", sensorId)
                        .with(csrf())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "value": 27.8,
                                  "dataType": "FLOAT",
                                  "unit": "C",
                                  "timestamp": "2026-04-17T12:10:00"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.value").value(27.8))
                .andExpect(jsonPath("$.data.dataType").value("FLOAT"))
                .andExpect(jsonPath("$.data.unit").value("C"));
    }

    @Test
    @DisplayName("문자형 이력 입력 성공")
    void createStringHistorySuccess() throws Exception {
        mockMvc.perform(post("/api/sensors/{sensorId}/history/string", sensorId)
                        .with(csrf())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "RUNNING",
                                  "timestamp": "2026-04-17T12:15:00"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("RUNNING"));
    }

    @Test
    @DisplayName("숫자형 이력 입력 실패 - 잘못된 dataType")
    void createNumericHistoryFailsForInvalidDataType() throws Exception {
        mockMvc.perform(post("/api/sensors/{sensorId}/history/numeric", sensorId)
                        .with(csrf())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "value": 27.8,
                                  "dataType": "STRING",
                                  "unit": "C"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("숫자형 이력 입력 실패 - 다른 사용자의 센서")
    void createNumericHistoryFailsForOtherUsersSensor() throws Exception {
        mockMvc.perform(post("/api/sensors/{sensorId}/history/numeric", otherUserSensorId)
                        .with(csrf())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "value": 27.8,
                                  "dataType": "FLOAT",
                                  "unit": "C"
                                }
                                """))
                .andExpect(status().isNotFound());
    }
}
