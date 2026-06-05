package com.festapp.dashboard.telemetry.config;

import com.festapp.dashboard.common.config.CorsProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

  private final CorsProperties corsProperties;

  @Override
  public void configureMessageBroker(MessageBrokerRegistry config) {
    config.enableSimpleBroker("/topic"); // 프론트엔드가 구독할 경로
    config.setApplicationDestinationPrefixes("/app"); // 프론트엔드가 메시지를 보낼 경로
  }

  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    registry.addEndpoint("/ws-stomp") // 최초 웹소켓 연결 주소
        .setAllowedOriginPatterns(corsProperties.getAllowedOriginPatterns().toArray(String[]::new))
        // SockJS credentials 요청은 wildcard Origin("*")을 사용할 수 없습니다.
        .withSockJS();
  }
}
