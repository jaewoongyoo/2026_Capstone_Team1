package com.festapp.dashboard.telemetry.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.festapp.dashboard.equipment.service.EquipmentService;
import com.festapp.dashboard.telemetry.dto.SensorDataPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.mqtt", name = "enabled", havingValue = "true")
public class MqttTelemetryConsumer {

    private static final String EQUIPMENT_TOPIC_PREFIX = "factory/equipment/";
    private static final String TELEMETRY_SUFFIX = "/telemetry";
    private static final String METADATA_SUFFIX = "/metadata";

    private final ObjectMapper objectMapper;
    private final RealTimeDataService realTimeDataService;
    private final EquipmentService equipmentService;

    @ServiceActivator(inputChannel = "mqttInputChannel")
    public void consume(Message<?> message) {
        String topic = message.getHeaders().get(MqttHeaders.RECEIVED_TOPIC, String.class);
        String payload = String.valueOf(message.getPayload());
        consume(topic, payload);
    }

    void consume(String topic, String payload) {
        if (topic == null || topic.isBlank()) {
            log.warn("MQTT payload skipped because topic is missing");
            return;
        }
        if (payload == null || payload.isBlank()) {
            log.warn("Blank MQTT payload skipped: topic={}", topic);
            return;
        }

        String equipmentName = extractEquipmentName(topic);
        if (equipmentName == null) {
            log.warn("MQTT payload skipped because topic is unsupported: {}", topic);
            return;
        }

        if (topic.endsWith(METADATA_SUFFIX)) {
            log.info("MQTT equipment metadata received: equipmentId={}, payload={}", equipmentName, payload);
            try {
                JsonNode root = objectMapper.readTree(payload);
                List<String> tags = new ArrayList<>();
                if (root.has("tags") && root.get("tags").isArray()) {
                    for (JsonNode tagNode : root.get("tags")) {
                        if (tagNode.isTextual()) {
                            tags.add(tagNode.asText());
                        }
                    }
                }
                if (root.has("sensors") && root.get("sensors").isArray()) {
                    for (JsonNode sensorNode : root.get("sensors")) {
                        String tagName = firstText(sensorNode, "sensorId", "name", "svid");
                        if (tagName != null && !tagName.isBlank()) {
                            tags.add(tagName);
                        }
                    }
                }
                equipmentService.upsertEquipmentMetadata(equipmentName, tags);
            } catch (JsonProcessingException e) {
                log.warn("Invalid MQTT metadata payload skipped: topic={}, payload={}", topic, payload, e);
            } catch (Exception e) {
                log.error("MQTT metadata payload processing failed: topic={}, payload={}", topic, payload, e);
            }
            return;
        }

        if (!topic.endsWith(TELEMETRY_SUFFIX)) {
            log.warn("MQTT payload skipped because topic is not telemetry: {}", topic);
            return;
        }

        try {
            SensorDataPayload sensorPayload = toSensorDataPayload(equipmentName, payload);
            boolean processed = realTimeDataService.processSensorData(sensorPayload);
            if (!processed) {
                log.warn("MQTT telemetry payload skipped by processor: equipmentId={}", sensorPayload.getEquipmentId());
            }
        } catch (JsonProcessingException e) {
            log.warn("Invalid MQTT telemetry payload skipped: topic={}, payload={}", topic, payload, e);
        } catch (Exception e) {
            log.error("MQTT telemetry payload processing failed: topic={}, payload={}", topic, payload, e);
        }
    }

    private SensorDataPayload toSensorDataPayload(String equipmentName, String payload)
            throws JsonProcessingException {
        JsonNode root = objectMapper.readTree(payload);
        SensorDataPayload sensorPayload;

        if (root.has("sensors")) {
            sensorPayload = SensorDataPayload.builder()
                    .equipmentEntityId(root.hasNonNull("equipmentEntityId") ? root.get("equipmentEntityId").asLong() : null)
                    .equipmentId(textOrNull(root, "equipmentId"))
                    .timestamp(textOrNull(root, "timestamp"))
                    .status(textOrNull(root, "status"))
                    .sensors(normalizeSensorArray(root.get("sensors")))
                    .build();
        } else {
            sensorPayload = SensorDataPayload.builder()
                    .timestamp(textOrNull(root, "timestamp"))
                    .status(textOrDefault(root, "status", "RUN"))
                    .sensors(flattenSensorValues(root))
                    .build();
        }

        if (sensorPayload.getEquipmentId() == null || sensorPayload.getEquipmentId().isBlank()) {
            sensorPayload.setEquipmentId(equipmentName);
        }
        if (sensorPayload.getStatus() == null || sensorPayload.getStatus().isBlank()) {
            sensorPayload.setStatus("RUN");
        }
        return sensorPayload;
    }

    private List<SensorDataPayload.SensorDetails> normalizeSensorArray(JsonNode sensorsNode) {
        List<SensorDataPayload.SensorDetails> sensors = new ArrayList<>();
        if (sensorsNode == null || !sensorsNode.isArray()) {
            return sensors;
        }

        for (JsonNode sensorNode : sensorsNode) {
            if (sensorNode == null || sensorNode.isNull()) {
                continue;
            }
            String sensorId = firstText(sensorNode, "sensorId", "name", "svid");
            JsonNode valueNode = sensorNode.get("value");
            if (sensorId == null || sensorId.isBlank() || valueNode == null || valueNode.isNull()) {
                continue;
            }
            String dataType = textOrNull(sensorNode, "dataType");
            sensors.add(SensorDataPayload.SensorDetails.builder()
                    .sensorId(sensorId)
                    .dataType(dataType)
                    .value(toSensorValue(valueNode, dataType))
                    .unit(textOrNull(sensorNode, "unit"))
                    .build());
        }
        return sensors;
    }

    private List<SensorDataPayload.SensorDetails> flattenSensorValues(JsonNode root) {
        List<SensorDataPayload.SensorDetails> sensors = new ArrayList<>();
        Iterator<Map.Entry<String, JsonNode>> fields = root.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            if (isMetadataField(field.getKey())) {
                continue;
            }
            JsonNode value = field.getValue();
            if (value == null || value.isNull() || value.isObject() || value.isArray()) {
                continue;
            }
            sensors.add(SensorDataPayload.SensorDetails.builder()
                    .sensorId(field.getKey())
                    .value(toPlainValue(value))
                    .build());
        }
        return sensors;
    }

    private Object toPlainValue(JsonNode value) {
        if (value.isNumber()) {
            return value.doubleValue();
        }
        if (value.isBoolean()) {
            return value.booleanValue();
        }
        return value.asText();
    }

    private Object toSensorValue(JsonNode value, String dataType) {
        if ("BOOLEAN".equalsIgnoreCase(dataType)) {
            if (value.isBoolean()) {
                return value.booleanValue();
            }
            if (value.isNumber()) {
                return value.asInt() != 0;
            }
            String text = value.asText();
            if ("1".equals(text)) {
                return true;
            }
            if ("0".equals(text)) {
                return false;
            }
            return Boolean.parseBoolean(text);
        }
        if ("INTEGER".equalsIgnoreCase(dataType) || "INT".equalsIgnoreCase(dataType)) {
            return value.isNumber() ? value.longValue() : value.asLong();
        }
        if ("FLOAT".equalsIgnoreCase(dataType) || "DOUBLE".equalsIgnoreCase(dataType)) {
            return value.isNumber() ? value.doubleValue() : value.asDouble();
        }
        return toPlainValue(value);
    }

    private boolean isMetadataField(String key) {
        return "equipmentId".equals(key)
                || "equipmentEntityId".equals(key)
                || "equipmentName".equals(key)
                || "timestamp".equals(key)
                || "status".equals(key);
    }

    private String extractEquipmentName(String topic) {
        if (!topic.startsWith(EQUIPMENT_TOPIC_PREFIX)) {
            return null;
        }
        if (!topic.endsWith(TELEMETRY_SUFFIX) && !topic.endsWith(METADATA_SUFFIX)) {
            return null;
        }
        String tail = topic.substring(EQUIPMENT_TOPIC_PREFIX.length());
        int slashIndex = tail.indexOf('/');
        if (slashIndex <= 0) {
            return null;
        }
        return tail.substring(0, slashIndex);
    }

    private String textOrNull(JsonNode root, String fieldName) {
        JsonNode node = root.get(fieldName);
        return node != null && !node.isNull() ? node.asText() : null;
    }

    private String textOrDefault(JsonNode root, String fieldName, String defaultValue) {
        String value = textOrNull(root, fieldName);
        return value != null && !value.isBlank() ? value : defaultValue;
    }

    private String firstText(JsonNode root, String... fieldNames) {
        for (String fieldName : fieldNames) {
            String value = textOrNull(root, fieldName);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
