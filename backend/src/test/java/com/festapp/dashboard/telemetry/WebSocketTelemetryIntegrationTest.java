package com.festapp.dashboard.telemetry;

import com.festapp.dashboard.dashboard.entity.Dashboard;
import com.festapp.dashboard.dashboard.repository.DashboardRepository;
import com.festapp.dashboard.equipment.entity.Equipment;
import com.festapp.dashboard.equipment.repository.EquipmentRepository;
import com.festapp.dashboard.telemetry.dto.SensorDataPayload;
import com.festapp.dashboard.telemetry.repository.SensorNumericHistoryRepository;
import com.festapp.dashboard.telemetry.repository.SensorRepository;
import com.festapp.dashboard.telemetry.repository.SensorStringHistoryRepository;
import com.festapp.dashboard.telemetry.service.RealTimeDataService;
import com.festapp.dashboard.user.entity.User;
import com.festapp.dashboard.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.lang.reflect.Type;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = "classpath:application-test.properties")
@DisplayName("Telemetry WebSocket 통합 테스트")
class WebSocketTelemetryIntegrationTest {

    @LocalServerPort
    private int port;

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

    private Equipment equipment;

    @BeforeEach
    void setUp() {
        sensorStringHistoryRepository.deleteAll();
        sensorNumericHistoryRepository.deleteAll();
        sensorRepository.deleteAll();
        equipmentRepository.deleteAll();
        dashboardRepository.deleteAll();
        userRepository.deleteAll();

        User user = userRepository.save(User.builder()
                .username("websocket-user")
                .email("websocket@example.com")
                .password("encoded")
                .fullName("WebSocket User")
                .role(User.Role.USER)
                .isActive(true)
                .build());

        Dashboard dashboard = dashboardRepository.save(Dashboard.builder()
                .dashboardName("WebSocket Dashboard")
                .description("WebSocket Dashboard")
                .user(user)
                .build());

        equipment = equipmentRepository.save(Equipment.builder()
                .equipmentName("CVD-WS-01")
                .field("CVD")
                .dashboard(dashboard)
                .build());
    }

    @Test
    @DisplayName("equipmentEntityId topic 구독자는 telemetry 브로드캐스트를 수신한다")
    void subscriberReceivesTelemetryByEquipmentEntityIdTopic() throws Exception {
        WebSocketStompClient stompClient = new WebSocketStompClient(new SockJsClient(List.of(
                new WebSocketTransport(new StandardWebSocketClient())
        )));
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        StompSession session = stompClient
                .connectAsync("ws://localhost:" + port + "/ws-stomp", new StompSessionHandlerAdapter() {})
                .get(5, TimeUnit.SECONDS);

        BlockingQueue<SensorDataPayload> messages = new LinkedBlockingQueue<>();
        session.subscribe("/topic/equipment-id/" + equipment.getEquipmentId(), new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return SensorDataPayload.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                messages.offer((SensorDataPayload) payload);
            }
        });

        SensorDataPayload payload = SensorDataPayload.builder()
                .equipmentEntityId(equipment.getEquipmentId())
                .equipmentId(equipment.getEquipmentName())
                .status("RUN")
                .sensors(List.of(SensorDataPayload.SensorDetails.builder()
                        .sensorId("Temp_0")
                        .value(971.5)
                        .unit("C")
                        .build()))
                .build();

        assertThat(realTimeDataService.processSensorData(payload)).isTrue();

        SensorDataPayload received = messages.poll(5, TimeUnit.SECONDS);
        assertThat(received).isNotNull();
        assertThat(received.getEquipmentEntityId()).isEqualTo(equipment.getEquipmentId());
        assertThat(received.getEquipmentId()).isEqualTo(equipment.getEquipmentName());
        assertThat(received.getSensors()).hasSize(1);

        session.disconnect();
        stompClient.stop();
    }
}
