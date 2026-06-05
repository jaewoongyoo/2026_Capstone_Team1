package com.festapp.dashboard.equipment.service;

import com.festapp.dashboard.common.exception.ErrorCode;
import com.festapp.dashboard.common.exception.ResourceNotFoundException;
import com.festapp.dashboard.dashboard.entity.Dashboard;
import com.festapp.dashboard.dashboard.repository.DashboardRepository;
import com.festapp.dashboard.dashboard.service.DashboardProvisioningService;
import com.festapp.dashboard.dashboard.widget.repository.DashboardWidgetRepository;
import com.festapp.dashboard.equipment.dto.DiscoveryApplyRequest;
import com.festapp.dashboard.equipment.dto.DiscoveryApplyResponse;
import com.festapp.dashboard.equipment.dto.EquipmentCurrentResponse;
import com.festapp.dashboard.equipment.dto.EquipmentRequest;
import com.festapp.dashboard.equipment.dto.EquipmentResponse;
import com.festapp.dashboard.equipment.entity.Equipment;
import com.festapp.dashboard.equipment.repository.EquipmentRepository;
import com.festapp.dashboard.telemetry.dto.SensorDataPayload;
import com.festapp.dashboard.telemetry.dto.SensorResponse;
import com.festapp.dashboard.telemetry.entity.Sensor;
import com.festapp.dashboard.telemetry.repository.SensorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class EquipmentService {

    private final EquipmentRepository equipmentRepository;
    private final DashboardRepository dashboardRepository;
    private final SensorRepository sensorRepository;
    private final DashboardWidgetRepository dashboardWidgetRepository;
    private final DashboardProvisioningService dashboardProvisioningService;
    private final RedisTemplate<String, Object> redisTemplate;

    public void upsertEquipmentMetadata(String equipmentName, List<String> tagNames) {
        List<Equipment> existingEquipments = equipmentRepository.findByEquipmentName(equipmentName);
        
        if (existingEquipments.isEmpty()) {
            Dashboard defaultDashboard = dashboardProvisioningService.getSystemDefaultDashboard();
            Equipment newEquipment = equipmentRepository.save(Equipment.builder()
                    .dashboard(defaultDashboard)
                    .equipmentName(equipmentName)
                    .build());
            existingEquipments = List.of(newEquipment);
        }
        
        if (tagNames != null) {
            for (Equipment equipment : existingEquipments) {
                for (String tagName : tagNames) {
                    sensorRepository.findBySensorNameAndEquipmentEquipmentId(tagName, equipment.getEquipmentId())
                            .orElseGet(() -> sensorRepository.save(Sensor.builder()
                                    .equipment(equipment)
                                    .sensorName(tagName)
                                    .build()));
                }
            }
        }
    }

    public EquipmentResponse createEquipment(Long userId, EquipmentRequest request) {
        Dashboard dashboard = getDashboardOrThrow(userId, request.getDashboardId());
        Equipment equipment = Equipment.builder()
                .dashboard(dashboard)
                .equipmentName(request.getEquipmentName())
                .field(request.getField())
                .build();
        return EquipmentResponse.fromEntity(equipmentRepository.save(equipment));
    }

    public DiscoveryApplyResponse applyDiscovery(Long userId, DiscoveryApplyRequest request) {
        Dashboard dashboard = getDashboardOrThrow(userId, request.getDashboardId());
        List<DiscoveryApplyResponse.AppliedEquipment> appliedEquipment = new ArrayList<>();

        for (DiscoveryApplyRequest.DiscoveredEquipment discovered : request.getAssets()) {
            Equipment equipment = equipmentRepository
                    .findByEquipmentNameAndDashboardDashboardId(discovered.getEquipmentName(), dashboard.getDashboardId())
                    .map(existing -> updateDiscoveredEquipment(existing, discovered))
                    .orElseGet(() -> createDiscoveredEquipment(dashboard, discovered));

            List<SensorResponse> sensors = applyDiscoveredSensors(equipment, discovered.getTags());
            appliedEquipment.add(DiscoveryApplyResponse.AppliedEquipment.builder()
                    .equipment(EquipmentResponse.fromEntity(equipment))
                    .sensors(sensors)
                    .build());
        }

        int sensorCount = appliedEquipment.stream()
                .mapToInt(item -> item.getSensors().size())
                .sum();

        return DiscoveryApplyResponse.builder()
                .dashboardId(dashboard.getDashboardId())
                .equipmentCount(appliedEquipment.size())
                .sensorCount(sensorCount)
                .equipment(appliedEquipment)
                .build();
    }

    @Transactional(readOnly = true)
    public List<EquipmentResponse> getDashboardEquipment(Long userId, Long dashboardId) {
        getDashboardOrThrow(userId, dashboardId);
        return equipmentRepository.findByDashboardDashboardIdOrderByEquipmentIdAsc(dashboardId)
                .stream()
                .map(EquipmentResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EquipmentResponse> searchUserEquipment(Long userId, String keyword) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        List<Equipment> equipment = normalizedKeyword.isEmpty()
                ? equipmentRepository.findByDashboardUserUserIdOrderByEquipmentIdAsc(userId)
                : equipmentRepository.searchByUserAndKeyword(userId, normalizedKeyword);

        return equipment.stream()
                .map(EquipmentResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EquipmentResponse> searchDashboardEquipment(Long userId, Long dashboardId, String keyword) {
        getDashboardOrThrow(userId, dashboardId);
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        if (normalizedKeyword.isEmpty()) {
            return getDashboardEquipment(userId, dashboardId);
        }
        return equipmentRepository.searchByDashboardAndKeyword(dashboardId, userId, normalizedKeyword)
                .stream()
                .map(EquipmentResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public EquipmentResponse getEquipment(Long userId, Long equipmentId) {
        return EquipmentResponse.fromEntity(getEquipmentOrThrow(userId, equipmentId));
    }

    @Transactional(readOnly = true)
    public EquipmentCurrentResponse getEquipmentCurrent(Long userId, Long equipmentId) {
        Equipment equipment = getEquipmentOrThrow(userId, equipmentId);
        return EquipmentCurrentResponse.fromEntity(equipment, getCurrentPayload(equipment));
    }

    @Transactional(readOnly = true)
    public List<EquipmentCurrentResponse> getMyEquipmentCurrent(Long userId) {
        return equipmentRepository.findByDashboardUserUserIdOrderByEquipmentIdAsc(userId)
                .stream()
                .map(equipment -> EquipmentCurrentResponse.fromEntity(equipment, getCurrentPayload(equipment)))
                .toList();
    }

    public EquipmentResponse updateEquipment(Long userId, Long equipmentId, EquipmentRequest request) {
        Equipment equipment = getEquipmentOrThrow(userId, equipmentId);
        Dashboard dashboard = getDashboardOrThrow(userId, request.getDashboardId());
        equipment.setDashboard(dashboard);
        equipment.setEquipmentName(request.getEquipmentName());
        equipment.setField(request.getField());
        return EquipmentResponse.fromEntity(equipmentRepository.save(equipment));
    }

    public void deleteEquipment(Long userId, Long equipmentId) {
        getEquipmentOrThrow(userId, equipmentId);
        // 운영 데이터 보존을 위해 장비/센서/히스토리는 삭제하지 않고 화면에 배치된 위젯만 제거한다.
        dashboardWidgetRepository.deleteByUserIdAndEquipmentId(userId, equipmentId);
    }

    private Dashboard getDashboardOrThrow(Long userId, Long dashboardId) {
        return dashboardRepository.findByDashboardIdAndUserUserId(dashboardId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.DASHBOARD_NOT_FOUND));
    }

    private Equipment getEquipmentOrThrow(Long userId, Long equipmentId) {
        return equipmentRepository.findByEquipmentIdAndDashboardUserUserId(equipmentId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.EQUIPMENT_NOT_FOUND));
    }

    private SensorDataPayload getCurrentPayload(Equipment equipment) {
        Object cached = redisTemplate.opsForValue().get("equipment:current:id:" + equipment.getEquipmentId());
        if (!(cached instanceof SensorDataPayload)) {
            cached = redisTemplate.opsForValue().get("equipment:current:" + equipment.getEquipmentName());
        }
        return cached instanceof SensorDataPayload payload ? payload : null;
    }

    private Equipment createDiscoveredEquipment(Dashboard dashboard, DiscoveryApplyRequest.DiscoveredEquipment discovered) {
        return equipmentRepository.save(Equipment.builder()
                .dashboard(dashboard)
                .equipmentName(discovered.getEquipmentName())
                .field(discovered.getField())
                .build());
    }

    private Equipment updateDiscoveredEquipment(Equipment equipment, DiscoveryApplyRequest.DiscoveredEquipment discovered) {
        if (discovered.getField() != null && !discovered.getField().isBlank()) {
            equipment.setField(discovered.getField());
            return equipmentRepository.save(equipment);
        }
        return equipment;
    }

    private List<SensorResponse> applyDiscoveredSensors(
            Equipment equipment,
            List<DiscoveryApplyRequest.DiscoveredTag> tags) {
        if (tags == null || tags.isEmpty()) {
            return Collections.emptyList();
        }

        List<SensorResponse> sensors = new ArrayList<>();
        for (DiscoveryApplyRequest.DiscoveredTag tag : tags) {
            Sensor sensor = sensorRepository
                    .findBySensorNameAndEquipmentEquipmentId(tag.getSensorName(), equipment.getEquipmentId())
                    .orElseGet(() -> sensorRepository.save(Sensor.builder()
                            .equipment(equipment)
                            .sensorName(tag.getSensorName())
                            .build()));
            sensors.add(SensorResponse.fromEntity(sensor));
        }
        return sensors;
    }
}
