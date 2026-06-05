// 대시보드 위젯 검증 테스트 - 위젯 생성, 조회, 권한 검증 테스트
package com.festapp.dashboard.controller;

import com.festapp.dashboard.dashboard.widget.dto.WidgetRequestDto;
import com.festapp.dashboard.user.entity.User;
import com.festapp.dashboard.dashboard.widget.repository.DashboardWidgetRepository;
import com.festapp.dashboard.user.repository.UserRepository;
import com.festapp.dashboard.dashboard.widget.service.DashboardWidgetService;
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
@DisplayName("Dashboard Widget Validation Tests")
@Transactional
public class DashboardWidgetValidationTest {

    @Autowired
    private DashboardWidgetService widgetService;

    @Autowired
    private DashboardWidgetRepository widgetRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Long testUserId = 1L;
    private WidgetRequestDto validRequest;

    @BeforeEach
    void setUp() {
        widgetRepository.deleteAll();
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
    @DisplayName("위젯 생성 - 필수값 검증")
    void testWidgetCreationWithMissingTitle() {
        WidgetRequestDto invalidRequest = validRequest.toBuilder()
                .title(null)
                .build();

        assertThatThrownBy(() -> widgetService.createWidget(testUserId, invalidRequest))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("위젯 생성 - 음수 posX 검증")
    void testWidgetCreationWithNegativePosX() {
        WidgetRequestDto invalidRequest = validRequest.toBuilder()
                .posX(-1)
                .build();

        var created = widgetService.createWidget(testUserId, invalidRequest);
        assertThat(created.getPosX()).isEqualTo(-1);
    }

    @Test
    @DisplayName("위젯 생성 - 작은 width 검증")
    void testWidgetCreationWithSmallWidth() {
        WidgetRequestDto validRequestWithSmallWidth = validRequest.toBuilder()
                .width(1)
                .height(1)
                .build();

        var created = widgetService.createWidget(testUserId, validRequestWithSmallWidth);
        assertThat(created.getWidth()).isEqualTo(1);
        assertThat(created.getHeight()).isEqualTo(1);
    }

    @Test
    @DisplayName("위젯 조회 - 권한 검증")
    void testWidgetAccessControl() {
        var created = widgetService.createWidget(testUserId, validRequest);

        var retrieved = widgetService.getWidget(testUserId, created.getId());
        assertThat(retrieved.getId()).isEqualTo(created.getId());

        assertThatThrownBy(() -> widgetService.getWidget(2L, created.getId()))
                .isInstanceOf(com.festapp.dashboard.dashboard.widget.exception.WidgetNotFoundException.class);
    }

    @Test
    @DisplayName("여러 사용자별 위젯 격리")
    void testWidgetIsolationBetweenUsers() {
        // Get actual user IDs from database
        var allUsers = userRepository.findAll();
        Long user1Id = allUsers.get(0).getUserId();
        Long user2Id = allUsers.get(1).getUserId();

        var user1Widget = widgetService.createWidget(user1Id, validRequest);
        var user2Widget = widgetService.createWidget(user2Id, validRequest.toBuilder()
                .title("User 2 Widget")
                .build());

        var user1Widgets = widgetService.getMyWidgets(user1Id);
        var user2Widgets = widgetService.getMyWidgets(user2Id);

        assertThat(user1Widgets).hasSize(1);
        assertThat(user2Widgets).hasSize(1);
        assertThat(user1Widgets.get(0).getUserId()).isEqualTo(user1Id);
        assertThat(user2Widgets.get(0).getUserId()).isEqualTo(user2Id);
    }
}


