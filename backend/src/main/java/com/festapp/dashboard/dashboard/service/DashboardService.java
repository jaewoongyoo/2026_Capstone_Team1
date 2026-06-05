package com.festapp.dashboard.dashboard.service;

import com.festapp.dashboard.common.exception.ErrorCode;
import com.festapp.dashboard.common.exception.ResourceNotFoundException;
import com.festapp.dashboard.dashboard.dto.DashboardRequest;
import com.festapp.dashboard.dashboard.dto.DashboardResponse;
import com.festapp.dashboard.dashboard.entity.Dashboard;
import com.festapp.dashboard.dashboard.repository.DashboardRepository;
import com.festapp.dashboard.dashboard.widget.repository.DashboardWidgetRepository;
import com.festapp.dashboard.equipment.entity.Equipment;
import com.festapp.dashboard.equipment.repository.EquipmentRepository;
import com.festapp.dashboard.telemetry.entity.Sensor;
import com.festapp.dashboard.telemetry.repository.SensorNumericHistoryRepository;
import com.festapp.dashboard.telemetry.repository.SensorRepository;
import com.festapp.dashboard.telemetry.repository.SensorStringHistoryRepository;
import com.festapp.dashboard.user.entity.User;
import com.festapp.dashboard.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import org.springframework.data.redis.core.RedisTemplate;
import com.festapp.dashboard.dashboard.dto.PublicDashboardResponse;
import com.festapp.dashboard.dashboard.widget.dto.WidgetResponseDto;
import com.festapp.dashboard.dashboard.widget.entity.DashboardWidget;
import com.festapp.dashboard.equipment.dto.EquipmentCurrentResponse;
import com.festapp.dashboard.telemetry.dto.SensorDataPayload;

@Service
@RequiredArgsConstructor
@Transactional
public class DashboardService {

    private final DashboardRepository dashboardRepository;
    private final UserRepository userRepository;
    private final EquipmentRepository equipmentRepository;
    private final SensorRepository sensorRepository;
    private final SensorNumericHistoryRepository sensorNumericHistoryRepository;
    private final SensorStringHistoryRepository sensorStringHistoryRepository;
    private final DashboardWidgetRepository dashboardWidgetRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    public DashboardResponse createDashboard(Long userId, DashboardRequest request) {
        User user = getUserOrThrow(userId);
        Dashboard dashboard = Dashboard.builder()
                .dashboardName(request.getDashboardName())
                .description(request.getDescription())
                .user(user)
                .build();
        return DashboardResponse.fromEntity(dashboardRepository.save(dashboard));
    }

    @Transactional(readOnly = true)
    public List<DashboardResponse> getMyDashboards(Long userId) {
        return dashboardRepository.findByUserUserIdOrderByDashboardIdAsc(userId)
                .stream()
                .map(DashboardResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard(Long userId, Long dashboardId) {
        return DashboardResponse.fromEntity(getDashboardOrThrow(userId, dashboardId));
    }

    public DashboardResponse updateDashboard(Long userId, Long dashboardId, DashboardRequest request) {
        Dashboard dashboard = getDashboardOrThrow(userId, dashboardId);
        dashboard.setDashboardName(request.getDashboardName());
        dashboard.setDescription(request.getDescription());
        return DashboardResponse.fromEntity(dashboardRepository.save(dashboard));
    }

    public void deleteDashboard(Long userId, Long dashboardId) {
        Dashboard dashboard = getDashboardOrThrow(userId, dashboardId);
        List<Equipment> equipment = equipmentRepository.findByDashboardDashboardIdOrderByEquipmentIdAsc(dashboardId);

        dashboardWidgetRepository.deleteAllInBatch(
                dashboardWidgetRepository.findByDashboardDashboardIdOrderByWidgetIdAsc(dashboardId)
        );

        for (Equipment item : equipment) {
            List<Sensor> sensors = sensorRepository.findByEquipmentEquipmentIdOrderBySensorIdAsc(item.getEquipmentId());
            for (Sensor sensor : sensors) {
                sensorNumericHistoryRepository.deleteBySensorId(sensor.getSensorId());
                sensorStringHistoryRepository.deleteBySensorId(sensor.getSensorId());
            }
            sensorRepository.deleteAllInBatch(sensors);
        }

        equipmentRepository.deleteAllInBatch(equipment);
        dashboardRepository.delete(dashboard);
    }

    public DashboardResponse enableShare(Long userId, Long dashboardId) {
        Dashboard dashboard = getDashboardOrThrow(userId, dashboardId);
        dashboard.setShareToken(UUID.randomUUID().toString());
        dashboard.setIsPublic(true);
        return DashboardResponse.fromEntity(dashboardRepository.save(dashboard));
    }

    public DashboardResponse disableShare(Long userId, Long dashboardId) {
        Dashboard dashboard = getDashboardOrThrow(userId, dashboardId);
        dashboard.setShareToken(null);
        dashboard.setIsPublic(false);
        return DashboardResponse.fromEntity(dashboardRepository.save(dashboard));
    }

    @Transactional(readOnly = true)
    public PublicDashboardResponse getPublicDashboard(String shareToken) {
        Dashboard dashboard = dashboardRepository.findByShareToken(shareToken)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.DASHBOARD_NOT_FOUND));
        if (!Boolean.TRUE.equals(dashboard.getIsPublic())) {
            throw new ResourceNotFoundException(ErrorCode.DASHBOARD_NOT_FOUND);
        }

        Long dashboardId = dashboard.getDashboardId();

        // 1. 위젯 목록 조회 및 변환
        List<WidgetResponseDto> widgets = dashboardWidgetRepository.findByDashboardDashboardIdOrderByWidgetIdAsc(dashboardId)
                .stream()
                .map(WidgetResponseDto::fromEntity)
                .toList();

        // 2. 장비 목록 조회 및 실시간 데이터(Redis) 매핑
        List<EquipmentCurrentResponse> equipmentCurrent = equipmentRepository.findByDashboardDashboardIdOrderByEquipmentIdAsc(dashboardId)
                .stream()
                .map(equipment -> EquipmentCurrentResponse.fromEntity(equipment, getCurrentPayload(equipment)))
                .toList();

        return PublicDashboardResponse.of(dashboard, widgets, equipmentCurrent);
    }

    private SensorDataPayload getCurrentPayload(Equipment equipment) {
        Object cached = redisTemplate.opsForValue().get("equipment:current:id:" + equipment.getEquipmentId());
        if (!(cached instanceof SensorDataPayload)) {
            cached = redisTemplate.opsForValue().get("equipment:current:" + equipment.getEquipmentName());
        }
        return cached instanceof SensorDataPayload payload ? payload : null;
    }

    private Dashboard getDashboardOrThrow(Long userId, Long dashboardId) {
        return dashboardRepository.findByDashboardIdAndUserUserId(dashboardId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.DASHBOARD_NOT_FOUND));
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND));
    }
}
