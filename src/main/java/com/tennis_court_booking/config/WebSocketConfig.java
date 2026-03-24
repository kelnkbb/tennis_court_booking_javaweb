package com.tennis_court_booking.config;

import com.tennis_court_booking.websocket.JwtHandshakeInterceptor;
import com.tennis_court_booking.websocket.NotificationWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket  // 启用原生 WebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final NotificationWebSocketHandler notificationWebSocketHandler;
    private final JwtHandshakeInterceptor jwtHandshakeInterceptor;
    private final CorsProperties corsProperties;

    public WebSocketConfig(NotificationWebSocketHandler notificationWebSocketHandler,
                          JwtHandshakeInterceptor jwtHandshakeInterceptor,
                          CorsProperties corsProperties) {
        this.notificationWebSocketHandler = notificationWebSocketHandler;
        this.jwtHandshakeInterceptor = jwtHandshakeInterceptor;
        this.corsProperties = corsProperties;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(notificationWebSocketHandler, "/api/ws/notifications")
                .addInterceptors(jwtHandshakeInterceptor)
                .setAllowedOrigins(corsProperties.originArray());
    }
}
