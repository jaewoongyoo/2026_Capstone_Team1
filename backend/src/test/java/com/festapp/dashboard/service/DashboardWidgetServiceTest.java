// 대시보드 위젯 서비스 테스트 - 위젯 CRUD 기능 테스트
package com.festapp.dashboard.service;

import com.festapp.dashboard.dashboard.widget.dto.WidgetRequestDto;
import com.festapp.dashboard.dashboard.repository.DashboardRepository;
import com.festapp.dashboard.user.entity.User;
import com.festapp.dashboard.dashboard.widget.exception.WidgetNotFoundException;
import com.festapp.dashboard.dashboard.widget.repository.DashboardWidgetRepository;
import com.festapp.dashboard.dashboard.widget.service.DashboardWidgetService;
import com.festapp.dashboard.equipment.repository.EquipmentRepository;
import com.festapp.dashboard.telemetry.repository.SensorNumericHistoryRepository;
import com.festapp.dashboard.telemetry.repository.SensorRepository;
import com.festapp.dashboard.telemetry.repository.SensorStringHistoryRepository;
import com.festapp.dashboard.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
@DisplayName("Dashboard Widget Service Tests")
@Transactional
public class DashboardWidgetServiceTest {

    @Autowired
    private DashboardWidgetRepository widgetRepository;

    @Autowired
    private DashboardWidgetService widgetService;

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

    private Long testUserId = 1L;
    private Long otherUserId = 2L;
    private WidgetRequestDto validRequest;

    @BeforeEach
    void setUp() {
        widgetRepository.deleteAll();
        sensorStringHistoryRepository.deleteAll();
        sensorNumericHistoryRepository.deleteAll();
        sensorRepository.deleteAll();
        equipmentRepository.deleteAll();
        dashboardRepository.deleteAll();
        userRepository.deleteAll();

        // Create test users without explicitly setting userId
        User user1 = User.builder()
                .username("testuser1")
                .email("testuser1@example.com")
                .password(passwordEncoder.encode("password123"))
                .fullName("Test User 1")
                .role(User.Role.USER)
                .isActive(true)
                .build();

        User user2 = User.builder()
                .username("testuser2")
                .email("testuser2@example.com")
                .password(passwordEncoder.encode("password123"))
                .fullName("Test User 2")
                .role(User.Role.USER)
                .isActive(true)
                .build();

        user1 = userRepository.save(user1);
        user2 = userRepository.save(user2);
        testUserId = user1.getUserId();
        otherUserId = user2.getUserId();

        validRequest = WidgetRequestDto.builder()
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
    }

    @Test
    @DisplayName("위젯 생성 성공")
    void testCreateWidgetSuccess() {
        var created = widgetService.createWidget(testUserId, validRequest);

        assertThat(created).isNotNull();
        assertThat(created.getId()).isNotNull();
        assertThat(created.getTitle()).isEqualTo("Temperature Gauge");
        assertThat(created.getUserId()).isEqualTo(testUserId);
        assertThat(created.getEquipmentId()).isEqualTo("CVD-CHAMBER-01");
    }

    @Test
    @DisplayName("내 위젯 전체 조회 성공")
    void testGetMyWidgetsSuccess() {
        widgetService.createWidget(testUserId, validRequest);

        WidgetRequestDto request2 = validRequest.toBuilder()
                .equipmentId("CVD-CHAMBER-02")
                .title("Status Widget")
                .sensorId("Power_Status_001")
                .dataType("BOOLEAN")
                .unit("BOOL")
                .posX(0)
                .posY(2)
                .width(1)
                .height(1)
                .build();

        widgetService.createWidget(testUserId, request2);

        var myWidgets = widgetService.getMyWidgets(testUserId);

        assertThat(myWidgets).hasSize(2);
    }

    @Test
    @DisplayName("장비별 위젯 조회 성공")
    void testGetMyWidgetsByEquipmentSuccess() {
        widgetService.createWidget(testUserId, validRequest);

        WidgetRequestDto request2 = validRequest.toBuilder()
                .equipmentId("CVD-CHAMBER-02")
                .title("Different Equipment Widget")
                .build();

        widgetService.createWidget(testUserId, request2);

        var equipment1Widgets = widgetService.getMyWidgetsByEquipment(testUserId, "CVD-CHAMBER-01");
        var equipment2Widgets = widgetService.getMyWidgetsByEquipment(testUserId, "CVD-CHAMBER-02");

        assertThat(equipment1Widgets).hasSize(1);
        assertThat(equipment2Widgets).hasSize(1);
        assertThat(equipment1Widgets.get(0).getEquipmentId()).isEqualTo("CVD-CHAMBER-01");
    }

    @Test
    @DisplayName("위젯 단건 조회 성공")
    void testGetWidgetSuccess() {
        var createdWidget = widgetService.createWidget(testUserId, validRequest);
        Long widgetId = createdWidget.getId();

        var retrieved = widgetService.getWidget(testUserId, widgetId);

        assertThat(retrieved.getId()).isEqualTo(widgetId);
        assertThat(retrieved.getTitle()).isEqualTo("Temperature Gauge");
    }

    @Test
    @DisplayName("위젯 수정 성공")
    void testUpdateWidgetSuccess() {
        var createdWidget = widgetService.createWidget(testUserId, validRequest);
        Long widgetId = createdWidget.getId();

        WidgetRequestDto updateRequest = validRequest.toBuilder()
                .title("Updated Temperature Gauge")
                .posX(5)
                .posY(5)
                .build();

        var updated = widgetService.updateWidget(testUserId, widgetId, updateRequest);

        assertThat(updated.getTitle()).isEqualTo("Updated Temperature Gauge");
        assertThat(updated.getPosX()).isEqualTo(5);
        assertThat(updated.getPosY()).isEqualTo(5);
    }

    @Test
    @DisplayName("위젯 삭제 성공")
    void testDeleteWidgetSuccess() {
        var createdWidget = widgetService.createWidget(testUserId, validRequest);
        Long widgetId = createdWidget.getId();

        widgetService.deleteWidget(testUserId, widgetId);

        assertThatThrownBy(() -> widgetService.getWidget(testUserId, widgetId))
                .isInstanceOf(WidgetNotFoundException.class);
    }

    @Test
    @DisplayName("다른 사용자 위젯 접근 실패")
    void testAccessOtherUserWidgetThrowsException() {
        var created = widgetService.createWidget(testUserId, validRequest);

        assertThatThrownBy(() -> widgetService.getWidget(otherUserId, created.getId()))
                .isInstanceOf(WidgetNotFoundException.class);
    }

    @Test
    @DisplayName("존재하지 않는 위젯 조회 실패")
    void testGetNonExistentWidgetThrowsException() {
        assertThatThrownBy(() -> widgetService.getWidget(testUserId, 99999L))
                .isInstanceOf(WidgetNotFoundException.class);
    }

    @Test
    @DisplayName("위젯 레이아웃 일괄 저장 성공")
    void testUpdateLayoutsSuccess() {
        var widget1 = widgetService.createWidget(testUserId, validRequest);
        var widget2 = widgetService.createWidget(testUserId, validRequest.toBuilder()
                .title("Widget 2")
                .equipmentId("CVD-CHAMBER-02")
                .build());

        var layoutDto = com.festapp.dashboard.dashboard.widget.dto.WidgetLayoutUpdateDto.builder()
                .layouts(java.util.Arrays.asList(
                        com.festapp.dashboard.dashboard.widget.dto.WidgetLayoutUpdateDto.LayoutItem.builder()
                                .widgetId(widget1.getId())
                                .posX(10)
                                .posY(10)
                                .width(3)
                                .height(3)
                                .build(),
                        com.festapp.dashboard.dashboard.widget.dto.WidgetLayoutUpdateDto.LayoutItem.builder()
                                .widgetId(widget2.getId())
                                .posX(20)
                                .posY(20)
                                .width(2)
                                .height(2)
                                .build()
                ))
                .build();

        var updated = widgetService.updateLayouts(testUserId, layoutDto);

        assertThat(updated).hasSize(2);

        var updated1 = widgetService.getWidget(testUserId, widget1.getId());
        assertThat(updated1.getPosX()).isEqualTo(10);
        assertThat(updated1.getPosY()).isEqualTo(10);
        assertThat(updated1.getWidth()).isEqualTo(3);
        assertThat(updated1.getHeight()).isEqualTo(3);
    }
}

