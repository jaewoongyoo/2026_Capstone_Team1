package com.festapp.dashboard.telemetry.config;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.messaging.MessageChannel;
import org.springframework.util.StringUtils;

import java.util.Arrays;

@Configuration
@EnableIntegration
@ConditionalOnProperty(prefix = "app.mqtt", name = "enabled", havingValue = "true")
public class MqttConfig {

    @Bean
    public MessageChannel mqttInputChannel() {
        return new DirectChannel();
    }

    @Bean
    public DefaultMqttPahoClientFactory mqttClientFactory(
            @Value("${app.mqtt.broker-url}") String brokerUrl,
            @Value("${app.mqtt.username:}") String username,
            @Value("${app.mqtt.password:}") String password) {
        MqttConnectOptions options = new MqttConnectOptions();
        options.setServerURIs(new String[]{brokerUrl});
        options.setAutomaticReconnect(true);
        options.setCleanSession(true);

        if (StringUtils.hasText(username)) {
            options.setUserName(username);
        }
        if (StringUtils.hasText(password)) {
            options.setPassword(password.toCharArray());
        }

        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
        factory.setConnectionOptions(options);
        return factory;
    }

    @Bean
    public MessageProducer mqttInbound(
            DefaultMqttPahoClientFactory mqttClientFactory,
            MessageChannel mqttInputChannel,
            @Value("${app.mqtt.client-id}") String clientId,
            @Value("${app.mqtt.topics}") String topics,
            @Value("${app.mqtt.qos}") int qos) {
        String[] topicNames = Arrays.stream(topics.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toArray(String[]::new);
        int[] qosValues = new int[topicNames.length];
        Arrays.fill(qosValues, qos);

        MqttPahoMessageDrivenChannelAdapter adapter =
                new MqttPahoMessageDrivenChannelAdapter(clientId, mqttClientFactory, topicNames);
        adapter.setCompletionTimeout(5_000);
        adapter.setConverter(new DefaultPahoMessageConverter());
        adapter.setQos(qosValues);
        adapter.setOutputChannel(mqttInputChannel);
        return adapter;
    }
}
