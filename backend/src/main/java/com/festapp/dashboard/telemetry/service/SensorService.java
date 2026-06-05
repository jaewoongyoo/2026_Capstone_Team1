package com.festapp.dashboard.telemetry.service;

import com.festapp.dashboard.common.exception.ErrorCode;
import com.festapp.dashboard.common.exception.ResourceNotFoundException;
import com.festapp.dashboard.dashboard.widget.repository.DashboardWidgetRepository;
import com.festapp.dashboard.equipment.entity.Equipment;
import com.festapp.dashboard.equipment.repository.EquipmentRepository;
import com.festapp.dashboard.telemetry.dto.SensorRequest;
import com.festapp.dashboard.telemetry.dto.SensorResponse;
import com.festapp.dashboard.telemetry.entity.Sensor;
import com.festapp.dashboard.telemetry.repository.SensorNumericHistoryRepository;
import com.festapp.dashboard.telemetry.repository.SensorRepository;
import com.festapp.dashboard.telemetry.repository.SensorStringHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class SensorService {

    private final SensorRepository sensorRepository;
    private final EquipmentRepository equipmentRepository;
    private final SensorNumericHistoryRepository sensorNumericHistoryRepository;
    private final SensorStringHistoryRepository sensorStringHistoryRepository;
    private final DashboardWidgetRepository dashboardWidgetRepository;

    public SensorResponse createSensor(Long userId, SensorRequest request) {
        Equipment equipment = getEquipmentOrThrow(userId, request.getEquipmentId());
        Sensor sensor = Sensor.builder()
                .equipment(equipment)
                .sensorName(request.getSensorName())
                .build();
        return SensorResponse.fromEntity(sensorRepository.save(sensor));
    }

    @Transactional(readOnly = true)
    public List<SensorResponse> getEquipmentSensors(Long userId, Long equipmentId) {
        getEquipmentOrThrow(userId, equipmentId);
        return sensorRepository.findByEquipmentEquipmentIdOrderBySensorIdAsc(equipmentId)
                .stream()
                .map(SensorResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SensorResponse> getEquipmentSensorsByName(Long userId, String equipmentName) {
        Equipment equipment = getEquipmentByNameOrThrow(userId, equipmentName);
        return sensorRepository.findByEquipmentEquipmentIdOrderBySensorIdAsc(equipment.getEquipmentId())
                .stream()
                .map(SensorResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SensorResponse> searchUserSensors(Long userId, String keyword) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        List<Sensor> sensors = normalizedKeyword.isEmpty()
                ? sensorRepository.findByEquipmentDashboardUserUserIdOrderBySensorIdAsc(userId)
                : sensorRepository.searchByUserAndKeyword(userId, normalizedKeyword);

        return sensors.stream()
                .map(SensorResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SensorResponse> searchEquipmentSensors(Long userId, Long equipmentId, String keyword) {
        getEquipmentOrThrow(userId, equipmentId);
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        List<Sensor> sensors = normalizedKeyword.isEmpty()
                ? sensorRepository.findByEquipmentEquipmentIdOrderBySensorIdAsc(equipmentId)
                : sensorRepository.searchByEquipmentAndKeyword(equipmentId, userId, normalizedKeyword);

        return sensors.stream()
                .map(SensorResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SensorResponse> searchEquipmentSensorsByName(Long userId, String equipmentName, String keyword) {
        Equipment equipment = getEquipmentByNameOrThrow(userId, equipmentName);
        return searchEquipmentSensors(userId, equipment.getEquipmentId(), keyword);
    }

    @Transactional(readOnly = true)
    public SensorResponse getSensor(Long userId, Long sensorId) {
        return SensorResponse.fromEntity(getSensorOrThrow(userId, sensorId));
    }

    public SensorResponse updateSensor(Long userId, Long sensorId, SensorRequest request) {
        Sensor sensor = getSensorOrThrow(userId, sensorId);
        Equipment equipment = getEquipmentOrThrow(userId, request.getEquipmentId());
        sensor.setEquipment(equipment);
        sensor.setSensorName(request.getSensorName());
        return SensorResponse.fromEntity(sensorRepository.save(sensor));
    }

    public void deleteSensor(Long userId, Long sensorId) {
        Sensor sensor = getSensorOrThrow(userId, sensorId);
        dashboardWidgetRepository.clearSensorReference(sensorId);
        sensorNumericHistoryRepository.deleteBySensorId(sensorId);
        sensorStringHistoryRepository.deleteBySensorId(sensorId);
        sensorRepository.delete(sensor);
    }

    private Equipment getEquipmentOrThrow(Long userId, Long equipmentId) {
        return equipmentRepository.findByEquipmentIdAndDashboardUserUserId(equipmentId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.EQUIPMENT_NOT_FOUND));
    }

    private Equipment getEquipmentByNameOrThrow(Long userId, String equipmentName) {
        return equipmentRepository.findFirstByEquipmentNameAndDashboardUserUserId(equipmentName, userId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.EQUIPMENT_NOT_FOUND));
    }

    private Sensor getSensorOrThrow(Long userId, Long sensorId) {
        return sensorRepository.findBySensorIdAndEquipmentDashboardUserUserId(sensorId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.SENSOR_NOT_FOUND));
    }
}
