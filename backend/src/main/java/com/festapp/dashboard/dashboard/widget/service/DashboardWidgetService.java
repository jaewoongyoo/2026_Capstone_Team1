package com.festapp.dashboard.dashboard.widget.service;

import com.festapp.dashboard.common.exception.ErrorCode;
import com.festapp.dashboard.common.exception.ResourceNotFoundException;
import com.festapp.dashboard.dashboard.entity.Dashboard;
import com.festapp.dashboard.dashboard.repository.DashboardRepository;
import com.festapp.dashboard.dashboard.widget.dto.WidgetLayoutUpdateDto;
import com.festapp.dashboard.dashboard.widget.dto.WidgetRequestDto;
import com.festapp.dashboard.dashboard.widget.dto.WidgetResponseDto;
import com.festapp.dashboard.dashboard.widget.dto.WidgetWebSocketMessage;
import com.festapp.dashboard.dashboard.widget.entity.DashboardWidget;
import com.festapp.dashboard.dashboard.widget.exception.WidgetNotFoundException;
import com.festapp.dashboard.dashboard.widget.repository.DashboardWidgetRepository;
import com.festapp.dashboard.equipment.entity.Equipment;
import com.festapp.dashboard.equipment.repository.EquipmentRepository;
import com.festapp.dashboard.telemetry.entity.Sensor;
import com.festapp.dashboard.telemetry.repository.SensorRepository;
import com.festapp.dashboard.user.entity.User;
import com.festapp.dashboard.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class DashboardWidgetService {

    private final DashboardWidgetRepository widgetRepository;
    private final DashboardRepository dashboardRepository;
    private final EquipmentRepository equipmentRepository;
    private final SensorRepository sensorRepository;
    private final UserRepository userRepository;
    private final CacheManager cacheManager;
    private final SimpMessagingTemplate messagingTemplate;

    private Dashboard getDashboardOrDefault(Long userId, Long dashboardId) {
        if (dashboardId == null) {
            return dashboardRepository.findFirstByUserUserIdOrderByDashboardIdAsc(userId)
                    .or(() -> userRepository.findById(userId).map(this::createDefaultDashboard))
                    .orElseThrow(() -> {
                        log.warn("No dashboard found for user: {}", userId);
                        return new ResourceNotFoundException(ErrorCode.DASHBOARD_NOT_FOUND);
                    });
        }

        return dashboardRepository.findByDashboardIdAndUserUserId(dashboardId, userId)
                .orElseThrow(() -> {
                    log.warn("Dashboard not found with id: {} for user: {}", dashboardId, userId);
                    return new ResourceNotFoundException(ErrorCode.DASHBOARD_NOT_FOUND);
                });
    }

    private Dashboard createDefaultDashboard(User user) {
        return dashboardRepository.save(
                Dashboard.builder()
                        .dashboardName(user.getUsername() + " Dashboard")
                        .description("Auto-provisioned dashboard for " + user.getUsername())
                        .user(user)
                        .build()
        );
    }

    private Equipment resolveEquipment(Long userId, Dashboard dashboard, WidgetRequestDto dto) {
        if (dto.getEquipmentEntityId() != null) {
            return equipmentRepository.findByEquipmentIdAndDashboardDashboardId(dto.getEquipmentEntityId(), dashboard.getDashboardId())
                    .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.EQUIPMENT_NOT_FOUND));
        }

        String equipmentId = dto.getEquipmentId();
        if (equipmentId == null || equipmentId.isBlank()) {
            return null;
        }

        return equipmentRepository.findByEquipmentNameAndDashboardDashboardId(equipmentId, dashboard.getDashboardId())
                .orElseGet(() -> equipmentRepository.save(
                        Equipment.builder()
                                .equipmentName(equipmentId)
                                .dashboard(dashboard)
                                .build()
                ));
    }

    private Sensor resolveSensor(Long userId, Equipment equipment, WidgetRequestDto dto) {
        if (dto.getSensorEntityId() != null) {
            Sensor sensor = sensorRepository.findBySensorIdAndEquipmentDashboardUserUserId(dto.getSensorEntityId(), userId)
                    .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.SENSOR_NOT_FOUND));

            if (equipment != null && !sensor.getEquipment().getEquipmentId().equals(equipment.getEquipmentId())) {
                throw new ResourceNotFoundException(ErrorCode.SENSOR_NOT_FOUND, "지정한 sensorEntityId가 equipment와 일치하지 않습니다");
            }

            return sensor;
        }

        String sensorId = dto.getSensorId();
        if (sensorId == null || sensorId.isBlank()) {
            return null;
        }

        if (equipment == null) {
            throw new ResourceNotFoundException(ErrorCode.EQUIPMENT_NOT_FOUND, "sensorId를 지정하려면 equipmentId도 필요합니다");
        }

        return sensorRepository.findBySensorNameAndEquipmentEquipmentId(sensorId, equipment.getEquipmentId())
                .orElseGet(() -> sensorRepository.save(
                        Sensor.builder()
                                .sensorName(sensorId)
                                .equipment(equipment)
                                .build()
                ));
    }

    private DashboardWidget getWidgetOrThrow(Long widgetId, Long userId) {
        return widgetRepository.findByWidgetIdAndDashboardUserUserId(widgetId, userId)
                .orElseThrow(() -> {
                    log.warn("Widget not found with id: {} for user: {}", widgetId, userId);
                    return new WidgetNotFoundException(ErrorCode.WIDGET_NOT_FOUND);
                });
    }

    public WidgetResponseDto createWidget(Long userId, WidgetRequestDto dto) {
        Dashboard dashboard = getDashboardOrDefault(userId, dto.getDashboardId());
        Equipment equipment = resolveEquipment(userId, dashboard, dto);
        Sensor sensor = resolveSensor(userId, equipment, dto);

        DashboardWidget widget = DashboardWidget.builder()
                .dashboard(dashboard)
                .equipment(equipment)
                .sensor(sensor)
                .widgetType(dto.getWidgetType())
                .title(dto.getTitle())
                .chartType(dto.getChartType())
                .dataType(dto.getDataType())
                .unit(dto.getUnit())
                .posX(dto.getPosX())
                .posY(dto.getPosY())
                .width(dto.getWidth())
                .height(dto.getHeight())
                .configJson(dto.getConfigJson())
                .build();

        DashboardWidget savedWidget = widgetRepository.save(widget);
        WidgetResponseDto response = WidgetResponseDto.fromEntity(savedWidget);

        evictWidgetCaches();
        broadcastWidgetUpdate(WidgetWebSocketMessage.MessageType.WIDGET_CREATED, response, userId);
        return response;
    }

    @Transactional(readOnly = true)
    public List<WidgetResponseDto> getMyWidgets(Long userId) {
        return widgetRepository.findByDashboardUserUserIdOrderByWidgetIdAsc(userId)
                .stream()
                .map(WidgetResponseDto::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<WidgetResponseDto> getDashboardWidgets(Long userId, Long dashboardId) {
        dashboardRepository.findByDashboardIdAndUserUserId(dashboardId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.DASHBOARD_NOT_FOUND));

        return widgetRepository.findByDashboardDashboardIdOrderByWidgetIdAsc(dashboardId)
                .stream()
                .map(WidgetResponseDto::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<WidgetResponseDto> getMyWidgetsByEquipment(Long userId, String equipmentId) {
        return widgetRepository.findByDashboardUserUserIdAndEquipmentEquipmentNameOrderByWidgetIdAsc(userId, equipmentId)
                .stream()
                .map(WidgetResponseDto::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<WidgetResponseDto> getMyWidgetsByEquipmentEntityId(Long userId, Long equipmentId) {
        equipmentRepository.findByEquipmentIdAndDashboardUserUserId(equipmentId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.EQUIPMENT_NOT_FOUND));

        return widgetRepository.findByDashboardUserUserIdAndEquipmentEquipmentIdOrderByWidgetIdAsc(userId, equipmentId)
                .stream()
                .map(WidgetResponseDto::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public WidgetResponseDto getWidget(Long userId, Long widgetId) {
        return WidgetResponseDto.fromEntity(getWidgetOrThrow(widgetId, userId));
    }

    public WidgetResponseDto updateWidget(Long userId, Long widgetId, WidgetRequestDto dto) {
        DashboardWidget widget = getWidgetOrThrow(widgetId, userId);
        Dashboard dashboard = getDashboardOrDefault(userId, dto.getDashboardId());
        Equipment equipment = resolveEquipment(userId, dashboard, dto);
        Sensor sensor = resolveSensor(userId, equipment, dto);

        widget.setDashboard(dashboard);
        widget.setEquipment(equipment);
        widget.setSensor(sensor);
        widget.setWidgetType(dto.getWidgetType());
        widget.setTitle(dto.getTitle());
        widget.setChartType(dto.getChartType());
        widget.setDataType(dto.getDataType());
        widget.setUnit(dto.getUnit());
        widget.setPosX(dto.getPosX());
        widget.setPosY(dto.getPosY());
        widget.setWidth(dto.getWidth());
        widget.setHeight(dto.getHeight());
        widget.setConfigJson(dto.getConfigJson());

        DashboardWidget updatedWidget = widgetRepository.save(widget);
        WidgetResponseDto response = WidgetResponseDto.fromEntity(updatedWidget);

        evictWidgetCaches();
        broadcastWidgetUpdate(WidgetWebSocketMessage.MessageType.WIDGET_UPDATED, response, userId);
        return response;
    }

    public void deleteWidget(Long userId, Long widgetId) {
        DashboardWidget widget = getWidgetOrThrow(widgetId, userId);
        Long equipmentEntityId = widget.getEquipment() != null ? widget.getEquipment().getEquipmentId() : null;
        String equipmentId = widget.getEquipment() != null ? widget.getEquipment().getEquipmentName() : null;

        widgetRepository.delete(widget);
        widgetRepository.flush();
        evictWidgetCaches();

        WidgetWebSocketMessage wsMessage = WidgetWebSocketMessage.builder()
                .messageType(WidgetWebSocketMessage.MessageType.WIDGET_DELETED)
                .widgetId(widgetId)
                .userId(userId)
                .equipmentId(equipmentId)
                .equipmentEntityId(equipmentEntityId)
                .timestamp(LocalDateTime.now())
                .build();

        messagingTemplate.convertAndSend("/topic/user/" + userId + "/widgets", wsMessage);
        broadcastToEquipmentTopics(equipmentId, equipmentEntityId, wsMessage);
    }

    public List<WidgetResponseDto> updateLayouts(Long userId, WidgetLayoutUpdateDto dto) {
        if (dto.getLayouts() == null || dto.getLayouts().isEmpty()) {
            return getMyWidgets(userId);
        }

        List<WidgetResponseDto> updatedWidgets = new ArrayList<>();

        for (WidgetLayoutUpdateDto.LayoutItem layout : dto.getLayouts()) {
            DashboardWidget widget = getWidgetOrThrow(layout.getWidgetId(), userId);
            widget.setPosX(layout.getPosX());
            widget.setPosY(layout.getPosY());
            widget.setWidth(layout.getWidth());
            widget.setHeight(layout.getHeight());

            DashboardWidget saved = widgetRepository.save(widget);
            WidgetResponseDto response = WidgetResponseDto.fromEntity(saved);
            updatedWidgets.add(response);

            broadcastWidgetUpdate(WidgetWebSocketMessage.MessageType.LAYOUT_UPDATED, response, userId);
        }

        evictWidgetCaches();
        return updatedWidgets;
    }

    private void evictWidgetCaches() {
        clearCache("widgets");
        clearCache("widgetsByEquipment");
    }

    private void clearCache(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
        }
    }

    private void broadcastWidgetUpdate(String messageType, WidgetResponseDto widget, Long userId) {
        WidgetWebSocketMessage wsMessage = WidgetWebSocketMessage.builder()
                .messageType(messageType)
                .widgetId(widget.getId())
                .userId(userId)
                .equipmentId(widget.getEquipmentId())
                .equipmentEntityId(widget.getEquipmentEntityId())
                .widget(widget)
                .timestamp(LocalDateTime.now())
                .build();

        messagingTemplate.convertAndSend("/topic/user/" + userId + "/widgets", wsMessage);
        broadcastToEquipmentTopics(widget.getEquipmentId(), widget.getEquipmentEntityId(), wsMessage);
    }

    private void broadcastToEquipmentTopics(String legacyEquipmentId, Long equipmentEntityId, WidgetWebSocketMessage message) {
        if (legacyEquipmentId != null) {
            messagingTemplate.convertAndSend("/topic/equipment/" + legacyEquipmentId + "/widgets", message);
        }
        if (equipmentEntityId != null) {
            messagingTemplate.convertAndSend("/topic/equipment-id/" + equipmentEntityId + "/widgets", message);
        }
    }
}
