package com.festapp.dashboard.telemetry.service;

import com.festapp.dashboard.equipment.entity.Equipment;
import com.festapp.dashboard.equipment.repository.EquipmentRepository;
import com.festapp.dashboard.telemetry.dto.SensorDataPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RealTimeDataService {

  private final SimpMessagingTemplate messagingTemplate;
  private final RedisTemplate<String, Object> redisTemplate;
  private final SensorHistoryService sensorHistoryService;
  private final EquipmentRepository equipmentRepository;
  private final ObjectProvider<KafkaTelemetryProducer> kafkaTelemetryProducerProvider;

  @Value("${app.kafka.sensor-topic:uemd.sensor.data}")
  private String kafkaSensorTopic;

  public boolean processSensorData(SensorDataPayload payload) {
    if (payload == null) {
      log.warn("Skipping telemetry processing because payload is null");
      return false;
    }

    String equipmentId = payload.getEquipmentId();
    Long equipmentEntityId = payload.getEquipmentEntityId();
    if (equipmentEntityId == null && (equipmentId == null || equipmentId.isBlank())) {
      log.warn("Skipping telemetry processing because equipment reference is missing");
      return false;
    }

    autoTagging(payload.getSensors());

    if (!sensorHistoryService.persistTelemetryPayload(payload)) {
      log.warn("Skipping telemetry snapshot and broadcast because equipment could not be resolved: {}", resolveLogEquipment(payload));
      return false;
    }

    try {
      updateCurrentSnapshot(payload);
    } catch (Exception e) {
      log.warn("Redis snapshot update skipped for equipment {}: {}", resolveLogEquipment(payload), e.getMessage());
    }

    // 2. 프론트엔드(WebSocket)로 실시간 데이터 브로드캐스팅
    // 프론트엔드는 /topic/equipment/CVD-CHAMBER-04 등을 구독
    if (equipmentId != null && !equipmentId.isBlank()) {
      messagingTemplate.convertAndSend("/topic/equipment/" + equipmentId, payload);
    }
    for (Long targetEquipmentId : resolveTargetEquipmentIds(payload)) {
      messagingTemplate.convertAndSend("/topic/equipment-id/" + targetEquipmentId, copyForEquipment(payload, targetEquipmentId));
    }

    KafkaTelemetryProducer kafkaTelemetryProducer = kafkaTelemetryProducerProvider.getIfAvailable();
    if (kafkaTelemetryProducer != null) {
      kafkaTelemetryProducer.publishTelemetry(kafkaSensorTopic, payload);
    }

    log.debug("Data processed and broadcasted for equipment: {}", resolveLogEquipment(payload));
    return true;
  }

  private void updateCurrentSnapshot(SensorDataPayload payload) {
    if (payload.getEquipmentEntityId() != null) {
      redisTemplate.opsForValue().set("equipment:current:id:" + payload.getEquipmentEntityId(), payload);
    }
    if (payload.getEquipmentId() != null && !payload.getEquipmentId().isBlank()) {
      redisTemplate.opsForValue().set("equipment:current:" + payload.getEquipmentId(), payload);
    }
    if (payload.getEquipmentEntityId() == null && payload.getEquipmentId() != null && !payload.getEquipmentId().isBlank()) {
      for (Long targetEquipmentId : resolveTargetEquipmentIds(payload)) {
        redisTemplate.opsForValue().set("equipment:current:id:" + targetEquipmentId, copyForEquipment(payload, targetEquipmentId));
      }
    }
  }

  private String resolveLogEquipment(SensorDataPayload payload) {
    if (payload.getEquipmentEntityId() != null) {
      return "id:" + payload.getEquipmentEntityId();
    }
    return payload.getEquipmentId();
  }

  private List<Long> resolveTargetEquipmentIds(SensorDataPayload payload) {
    if (payload.getEquipmentEntityId() != null) {
      return List.of(payload.getEquipmentEntityId());
    }
    if (payload.getEquipmentId() == null || payload.getEquipmentId().isBlank()) {
      return List.of();
    }
    return equipmentRepository.findByEquipmentName(payload.getEquipmentId())
            .stream()
            .map(Equipment::getEquipmentId)
            .toList();
  }

  private SensorDataPayload copyForEquipment(SensorDataPayload payload, Long equipmentEntityId) {
    return SensorDataPayload.builder()
            .equipmentEntityId(equipmentEntityId)
            .equipmentId(payload.getEquipmentId())
            .timestamp(payload.getTimestamp())
            .status(payload.getStatus())
            .sensors(payload.getSensors())
            .build();
  }

  private void autoTagging(List<SensorDataPayload.SensorDetails> sensors) {
    if (sensors == null) return;

    for (SensorDataPayload.SensorDetails sensor : sensors) {
      Object value = sensor.getValue();
      String unit = (sensor.getUnit() != null) ? sensor.getUnit().toUpperCase() : "";

      // 1. Boolean 판별 (논리형이거나 유닛이 BOOL/STATUS 인 경우)
      if (value instanceof Boolean || unit.contains("BOOL") || unit.contains("STATUS")) {
        sensor.setDataType("BOOLEAN");
        // 정수형(0, 1)으로 들어온 경우를 대비해 Boolean으로 형변환 로직을 추가할 수도 있습니다.
      }
      // 2. 숫자형 판별
      else if (value instanceof Number) {
        double doubleVal = ((Number) value).doubleValue();

        // 소수점 이하 자리가 있는지 확인
        if (doubleVal % 1 != 0) {
          sensor.setDataType("FLOAT");
        } else {
          sensor.setDataType("INTEGER");
        }
      }
      // 3. 그 외 문자열
      else {
        sensor.setDataType("STRING");
      }
    }
  }
}
