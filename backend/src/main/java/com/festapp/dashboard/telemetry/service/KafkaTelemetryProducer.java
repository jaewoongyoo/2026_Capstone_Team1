package com.festapp.dashboard.telemetry.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.festapp.dashboard.telemetry.dto.SensorDataPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.kafka", name = "enabled", havingValue = "true")
public class KafkaTelemetryProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void publishTelemetry(String topic, SensorDataPayload payload) {
        try {
            String message = objectMapper.writeValueAsString(payload);
            publish(topic, payload.getEquipmentId(), message);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize telemetry payload for Kafka: {}", e.getMessage(), e);
        }
    }

    public void publish(String topic, String key, String message) {
        CompletableFuture.runAsync(() -> {
            try {
                kafkaTemplate.send(topic, key, message)
                        .whenComplete((result, ex) -> {
                            if (ex == null) {
                                log.debug("Sent Kafka message to topic [{}] with key [{}]", topic, key);
                            } else {
                                log.error("Failed to send Kafka message to topic [{}]: {}", topic, ex.getMessage(), ex);
                            }
                        });
            } catch (Exception e) {
                log.error("Kafka publish error to topic [{}]: {}", topic, e.getMessage(), e);
            }
        });
    }
}
