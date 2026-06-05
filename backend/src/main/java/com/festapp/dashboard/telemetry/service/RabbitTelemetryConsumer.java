package com.festapp.dashboard.telemetry.service;

import com.festapp.dashboard.telemetry.config.RabbitConfig;
import com.festapp.dashboard.telemetry.dto.SensorDataPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.rabbit", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RabbitTelemetryConsumer {

  private final RealTimeDataService realTimeDataService;

  @RabbitListener(queues = RabbitConfig.SENSOR_QUEUE)
  public void consume(SensorDataPayload payload) {
    realTimeDataService.processSensorData(payload);
  }
}
