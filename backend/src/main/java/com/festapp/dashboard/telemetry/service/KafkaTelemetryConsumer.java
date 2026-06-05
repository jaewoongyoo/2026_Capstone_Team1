package com.festapp.dashboard.telemetry.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.festapp.dashboard.telemetry.dto.SensorDataPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.kafka", name = "enabled", havingValue = "true")
public class KafkaTelemetryConsumer {

  private final ObjectMapper objectMapper;
  private final RealTimeDataService realTimeDataService;

  @KafkaListener(
      topics = "${app.kafka.sensor-topic:uemd.sensor.data}",
      groupId = "${spring.kafka.consumer.group-id:dashboard-backend}")
  public void consume(String message) {
    if (message == null || message.isBlank()) {
      log.warn("Blank Kafka telemetry payload skipped");
      return;
    }

    try {
      SensorDataPayload payload = objectMapper.readValue(message, SensorDataPayload.class);
      boolean processed = realTimeDataService.processSensorData(payload);
      if (!processed) {
        log.warn("Kafka telemetry payload skipped by processor: equipmentEntityId={}, equipmentId={}",
            payload.getEquipmentEntityId(),
            payload.getEquipmentId());
      }
    } catch (JsonProcessingException e) {
      log.warn("Invalid Kafka telemetry payload skipped: {}", message, e);
    } catch (Exception e) {
      log.error("Kafka telemetry payload processing failed: {}", message, e);
    }
  }
}
