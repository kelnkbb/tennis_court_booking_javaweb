package com.tennis_court_booking.config;

import com.tennis_court_booking.cs.ws.CsWebSocketHandler;
import com.tennis_court_booking.websocket.JwtHandshakeInterceptor;
import com.tennis_court_booking.websocket.NotificationWebSocketHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import java.util.Arrays;

@Configuration
@EnableWebSocket  // 启用原生 WebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final NotificationWebSocketHandler notificationWebSocketHandler;
    private final CsWebSocketHandler csWebSocketHandler;
    private final JwtHandshakeInterceptor jwtHandshakeInterceptor;

    @Value("${app.websocket.allowed-origin-patterns:*}")
    private String allowedOriginPatterns;

    public WebSocketConfig(NotificationWebSocketHandler notificationWebSocketHandler,
                          CsWebSocketHandler csWebSocketHandler,
                          JwtHandshakeInterceptor jwtHandshakeInterceptor) {
        this.notificationWebSocketHandler = notificationWebSocketHandler;
        this.csWebSocketHandler = csWebSocketHandler;
        this.jwtHandshakeInterceptor = jwtHandshakeInterceptor;
    }

    private String[] allowedOriginPatternArray() {
        return Arrays.stream(allowedOriginPatterns.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        String[] patterns = allowedOriginPatternArray();
        registry.addHandler(notificationWebSocketHandler, "/api/ws/notifications")
                .addInterceptors(jwtHandshakeInterceptor)
                .setAllowedOriginPatterns(patterns);
        registry.addHandler(csWebSocketHandler, "/api/ws/cs")
                .addInterceptors(jwtHandshakeInterceptor)
                .setAllowedOriginPatterns(patterns);
    }
}
