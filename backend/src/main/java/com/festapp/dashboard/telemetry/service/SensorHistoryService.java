package com.festapp.dashboard.telemetry.service;

import com.festapp.dashboard.common.exception.ErrorCode;
import com.festapp.dashboard.common.exception.ResourceNotFoundException;
import com.festapp.dashboard.dashboard.entity.Dashboard;
import com.festapp.dashboard.dashboard.service.DashboardProvisioningService;
import com.festapp.dashboard.equipment.entity.Equipment;
import com.festapp.dashboard.equipment.repository.EquipmentRepository;
import com.festapp.dashboard.telemetry.dto.SensorDataPayload;
import com.festapp.dashboard.telemetry.dto.SensorNumericHistoryCreateRequest;
import com.festapp.dashboard.telemetry.dto.SensorNumericHistoryResponse;
import com.festapp.dashboard.telemetry.dto.SensorStringHistoryCreateRequest;
import com.festapp.dashboard.telemetry.dto.SensorStringHistoryResponse;
import com.festapp.dashboard.telemetry.entity.Sensor;
import com.festapp.dashboard.telemetry.entity.SensorNumericHistory;
import com.festapp.dashboard.telemetry.entity.SensorStringHistory;
import com.festapp.dashboard.telemetry.repository.SensorNumericHistoryRepository;
import com.festapp.dashboard.telemetry.repository.SensorRepository;
import com.festapp.dashboard.telemetry.repository.SensorStringHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SensorHistoryService {

    private final EquipmentRepository equipmentRepository;
    private final SensorRepository sensorRepository;
    private final SensorNumericHistoryRepository sensorNumericHistoryRepository;
    private final SensorStringHistoryRepository sensorStringHistoryRepository;
    private final DashboardProvisioningService dashboardProvisioningService;

    @Transactional
    public SensorNumericHistoryResponse createNumericHistory(Long userId, Long sensorId, SensorNumericHistoryCreateRequest request) {
        Sensor sensor = getSensorOrThrow(userId, sensorId);
        SensorNumericHistory saved = sensorNumericHistoryRepository.save(
                SensorNumericHistory.builder()
                        .sensor(sensor)
                        .value(request.getValue())
                        .dataType(request.getDataType())
                        .unit(request.getUnit())
                        .timestamp(request.getTimestamp() != null ? request.getTimestamp() : LocalDateTime.now())
                        .build()
        );
        return SensorNumericHistoryResponse.fromEntity(saved);
    }

    @Transactional
    public SensorStringHistoryResponse createStringHistory(Long userId, Long sensorId, SensorStringHistoryCreateRequest request) {
        Sensor sensor = getSensorOrThrow(userId, sensorId);
        SensorStringHistory saved = sensorStringHistoryRepository.save(
                SensorStringHistory.builder()
                        .sensor(sensor)
                        .status(request.getStatus())
                        .timestamp(request.getTimestamp() != null ? request.getTimestamp() : LocalDateTime.now())
                        .build()
        );
        return SensorStringHistoryResponse.fromEntity(saved);
    }

    @Transactional
    public boolean persistTelemetryPayload(SensorDataPayload payload) {
        List<Equipment> equipmentTargets = resolveEquipmentTargets(payload);

        if (equipmentTargets.isEmpty()) {
            return false;
        }

        LocalDateTime recordedAt = parseTimestamp(payload.getTimestamp());
        if (payload.getSensors() == null) {
            return true;
        }

        for (Equipment equipment : equipmentTargets) {
            for (SensorDataPayload.SensorDetails details : payload.getSensors()) {
                Sensor sensor = resolveOrCreateSensor(equipment, details.getSensorId());
                if (sensor == null) {
                    continue;
                }

                if (isNumericDataType(details.getDataType()) && details.getValue() instanceof Number number) {
                    sensorNumericHistoryRepository.save(
                            SensorNumericHistory.builder()
                                    .sensor(sensor)
                                    .value(number.doubleValue())
                                    .dataType(details.getDataType())
                                    .unit(details.getUnit())
                                    .timestamp(recordedAt)
                                    .build()
                    );
                } else {
                    sensorStringHistoryRepository.save(
                            SensorStringHistory.builder()
                                    .sensor(sensor)
                                    .status(details.getValue() != null ? String.valueOf(details.getValue()) : payload.getStatus())
                                    .timestamp(recordedAt)
                                    .build()
                    );
                }
            }
        }
        return true;
    }

    private List<Equipment> resolveEquipmentTargets(SensorDataPayload payload) {
        if (payload.getEquipmentEntityId() != null) {
            Equipment equipment = equipmentRepository.findById(payload.getEquipmentEntityId()).orElse(null);
            if (equipment == null) {
                log.warn("Skipping telemetry persistence because equipmentEntityId is not registered: {}", payload.getEquipmentEntityId());
                return List.of();
            }
            if (payload.getEquipmentId() != null
                    && !payload.getEquipmentId().isBlank()
                    && !equipment.getEquipmentName().equals(payload.getEquipmentId())) {
                log.warn(
                        "Skipping telemetry persistence because equipmentEntityId {} does not match equipmentId '{}'",
                        payload.getEquipmentEntityId(),
                        payload.getEquipmentId()
                );
                return List.of();
            }
            return List.of(equipment);
        }

        if (payload.getEquipmentId() == null || payload.getEquipmentId().isBlank()) {
            log.warn("Skipping telemetry persistence because equipment reference is missing");
            return List.of();
        }

        List<Equipment> matches = equipmentRepository.findByEquipmentName(payload.getEquipmentId());
        if (matches.isEmpty()) {
            Dashboard defaultDashboard = dashboardProvisioningService.getSystemDefaultDashboard();
            Equipment newEquipment = equipmentRepository.save(Equipment.builder()
                    .dashboard(defaultDashboard)
                    .equipmentName(payload.getEquipmentId())
                    .build());
            log.info("Auto-provisioned new equipment '{}' to system default dashboard", payload.getEquipmentId());
            return List.of(newEquipment);
        }
        if (matches.size() > 1) {
            log.info(
                    "Persisting name-only telemetry for {} equipment records named '{}'",
                    matches.size(),
                    payload.getEquipmentId()
            );
        }
        return matches;
    }

    public List<SensorNumericHistoryResponse> getNumericHistory(Long userId, Long sensorId, LocalDateTime from, LocalDateTime to) {
        getSensorOrThrow(userId, sensorId);
        if (from != null && to != null) {
            return sensorNumericHistoryRepository.findBySensorSensorIdAndTimestampBetweenOrderByTimestampDesc(sensorId, from, to)
                    .stream()
                    .map(SensorNumericHistoryResponse::fromEntity)
                    .toList();
        }
        return sensorNumericHistoryRepository.findTop100BySensorSensorIdOrderByTimestampDesc(sensorId)
                .stream()
                .map(SensorNumericHistoryResponse::fromEntity)
                .toList();
    }

    public List<SensorStringHistoryResponse> getStringHistory(Long userId, Long sensorId, LocalDateTime from, LocalDateTime to) {
        getSensorOrThrow(userId, sensorId);
        if (from != null && to != null) {
            return sensorStringHistoryRepository.findBySensorSensorIdAndTimestampBetweenOrderByTimestampDesc(sensorId, from, to)
                    .stream()
                    .map(SensorStringHistoryResponse::fromEntity)
                    .toList();
        }
        return sensorStringHistoryRepository.findTop100BySensorSensorIdOrderByTimestampDesc(sensorId)
                .stream()
                .map(SensorStringHistoryResponse::fromEntity)
                .toList();
    }

    private Sensor getSensorOrThrow(Long userId, Long sensorId) {
        return sensorRepository.findBySensorIdAndEquipmentDashboardUserUserId(sensorId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.SENSOR_NOT_FOUND));
    }

    private Sensor resolveOrCreateSensor(Equipment equipment, String sensorId) {
        if (sensorId == null || sensorId.isBlank()) {
            log.warn("Skipping telemetry persistence because sensorId is missing for equipment {}", equipment.getEquipmentName());
            return null;
        }

        return sensorRepository.findBySensorNameAndEquipmentEquipmentId(sensorId, equipment.getEquipmentId())
                .orElseGet(() -> sensorRepository.save(
                        Sensor.builder()
                                .sensorName(sensorId)
                                .equipment(equipment)
                                .build()
                ));
    }

    private LocalDateTime parseTimestamp(String timestamp) {
        if (timestamp == null || timestamp.isBlank()) {
            return LocalDateTime.now();
        }

        try {
            return OffsetDateTime.parse(timestamp).toLocalDateTime();
        } catch (DateTimeParseException ex) {
            try {
                return LocalDateTime.parse(timestamp);
            } catch (DateTimeParseException ignored) {
                log.warn("Failed to parse telemetry timestamp '{}', using now()", timestamp);
                return LocalDateTime.now();
            }
        }
    }

    private boolean isNumericDataType(String dataType) {
        if (dataType == null) {
            return false;
        }
        return "FLOAT".equalsIgnoreCase(dataType)
                || "INTEGER".equalsIgnoreCase(dataType)
                || "INT".equalsIgnoreCase(dataType)
                || "DOUBLE".equalsIgnoreCase(dataType);
    }
}
