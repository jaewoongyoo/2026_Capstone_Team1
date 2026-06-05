package com.festapp.dashboard.telemetry.service;

import com.festapp.dashboard.dashboard.entity.Dashboard;
import com.festapp.dashboard.dashboard.repository.DashboardRepository;
import com.festapp.dashboard.dashboard.widget.repository.DashboardWidgetRepository;
import com.festapp.dashboard.equipment.entity.Equipment;
import com.festapp.dashboard.equipment.repository.EquipmentRepository;
import com.festapp.dashboard.telemetry.dto.SensorDataPayload;
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
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
@DisplayName("실시간 데이터 이력 적재 통합 테스트")
class RealTimeDataServiceIntegrationTest {

    @Autowired
    private RealTimeDataService realTimeDataService;

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

    private Equipment equipment;

    @BeforeEach
    void setUp() {
        dashboardWidgetRepository.deleteAll();
        sensorStringHistoryRepository.deleteAll();
        sensorNumericHistoryRepository.deleteAll();
        sensorRepository.deleteAll();
        equipmentRepository.deleteAll();
        dashboardRepository.deleteAll();
        userRepository.deleteAll();

        User user = userRepository.save(User.builder()
                .username("telemetry-user")
                .email("telemetry@example.com")
                .password("encoded")
                .fullName("Telemetry User")
                .role(User.Role.USER)
                .isActive(true)
                .build());

        Dashboard dashboard = dashboardRepository.save(Dashboard.builder()
                .dashboardName("Telemetry Dashboard")
                .description("Telemetry Dashboard")
                .user(user)
                .build());

        equipment = equipmentRepository.save(Equipment.builder()
                .equipmentName("CVD-CHAMBER-04")
                .field("CVD")
                .dashboard(dashboard)
                .build());
    }

    @Test
    @DisplayName("숫자형과 문자형 센서 이력이 함께 저장된다")
    void persistNumericAndStringHistory() {
        SensorDataPayload payload = SensorDataPayload.builder()
                .equipmentEntityId(equipment.getEquipmentId())
                .equipmentId("CVD-CHAMBER-04")
                .timestamp("2026-04-17T10:00:00Z")
                .status("RUN")
                .sensors(List.of(
                        SensorDataPayload.SensorDetails.builder()
                                .sensorId("Temp_0")
                                .value(971.5)
                                .unit("C")
                                .build(),
                        SensorDataPayload.SensorDetails.builder()
                                .sensorId("Valve_Status")
                                .value("OPEN")
                                .unit("STATUS")
                                .build()
                ))
                .build();

        realTimeDataService.processSensorData(payload);

        assertThat(sensorRepository.findByEquipmentEquipmentIdOrderBySensorIdAsc(equipment.getEquipmentId())).hasSize(2);
        assertThat(sensorNumericHistoryRepository.findTop100BySensorSensorIdOrderByTimestampDesc(
                sensorRepository.findBySensorNameAndEquipmentEquipmentId("Temp_0", equipment.getEquipmentId()).orElseThrow().getSensorId()
        )).hasSize(1);
        assertThat(sensorStringHistoryRepository.findTop100BySensorSensorIdOrderByTimestampDesc(
                sensorRepository.findBySensorNameAndEquipmentEquipmentId("Valve_Status", equipment.getEquipmentId()).orElseThrow().getSensorId()
        )).hasSize(1);
    }

    @Test
    @DisplayName("장비명이 중복되어도 equipmentEntityId가 있으면 해당 장비에 저장된다")
    void persistByEquipmentEntityIdWhenEquipmentNameIsDuplicated() {
        Equipment duplicatedNameEquipment = createDuplicatedNameEquipment();

        SensorDataPayload payload = SensorDataPayload.builder()
                .equipmentEntityId(duplicatedNameEquipment.getEquipmentId())
                .equipmentId("CVD-CHAMBER-04")
                .timestamp("2026-04-17T10:00:00Z")
                .status("RUN")
                .sensors(List.of(
                        SensorDataPayload.SensorDetails.builder()
                                .sensorId("Pressure_0")
                                .value(2.4)
                                .unit("bar")
                                .build()
                ))
                .build();

        realTimeDataService.processSensorData(payload);

        assertThat(sensorRepository.findByEquipmentEquipmentIdOrderBySensorIdAsc(equipment.getEquipmentId())).isEmpty();
        assertThat(sensorRepository.findByEquipmentEquipmentIdOrderBySensorIdAsc(duplicatedNameEquipment.getEquipmentId())).hasSize(1);
    }

    @Test
    @DisplayName("장비명이 중복된 레거시 payload는 같은 이름의 모든 장비에 저장된다")
    void persistLegacyEquipmentNameToAllMatchingEquipment() {
        Equipment duplicatedNameEquipment = createDuplicatedNameEquipment();

        SensorDataPayload payload = SensorDataPayload.builder()
                .equipmentId("CVD-CHAMBER-04")
                .timestamp("2026-04-17T10:00:00Z")
                .status("RUN")
                .sensors(List.of(
                        SensorDataPayload.SensorDetails.builder()
                                .sensorId("Pressure_0")
                                .value(2.4)
                                .unit("bar")
                                .build()
                ))
                .build();

        realTimeDataService.processSensorData(payload);

        assertThat(sensorRepository.findByEquipmentEquipmentIdOrderBySensorIdAsc(equipment.getEquipmentId())).hasSize(1);
        assertThat(sensorRepository.findByEquipmentEquipmentIdOrderBySensorIdAsc(duplicatedNameEquipment.getEquipmentId())).hasSize(1);
        assertThat(sensorNumericHistoryRepository.findAll()).hasSize(2);
    }

    @Test
    @DisplayName("등록되지 않은 장비는 기본 대시보드에 자동 등록하고 이력을 저장한다")
    void autoProvisionUnknownEquipment() {
        SensorDataPayload payload = SensorDataPayload.builder()
                .equipmentId("UNKNOWN-EQUIPMENT")
                .timestamp("2026-04-17T10:00:00Z")
                .sensors(List.of(
                        SensorDataPayload.SensorDetails.builder()
                                .sensorId("Temp_0")
                                .value(12.3)
                                .unit("C")
                                .build()
                ))
                .build();

        realTimeDataService.processSensorData(payload);

        assertThat(equipmentRepository.findByEquipmentName("UNKNOWN-EQUIPMENT")).hasSize(1);
        assertThat(sensorRepository.findAll()).hasSize(1);
        assertThat(sensorNumericHistoryRepository.findAll()).hasSize(1);
    }

    private Equipment createDuplicatedNameEquipment() {
        User anotherUser = userRepository.save(User.builder()
                .username("another-telemetry-user")
                .email("another-telemetry@example.com")
                .password("encoded")
                .fullName("Another Telemetry User")
                .role(User.Role.USER)
                .isActive(true)
                .build());

        Dashboard anotherDashboard = dashboardRepository.save(Dashboard.builder()
                .dashboardName("Another Telemetry Dashboard")
                .description("Another Telemetry Dashboard")
                .user(anotherUser)
                .build());

        return equipmentRepository.save(Equipment.builder()
                .equipmentName("CVD-CHAMBER-04")
                .field("CVD")
                .dashboard(anotherDashboard)
                .build());
    }
}
