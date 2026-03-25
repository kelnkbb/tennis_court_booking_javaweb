package com.tennis_court_booking.config;

import com.tennis_court_booking.cs.ws.CsWebSocketHandler;
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
    private final CsWebSocketHandler csWebSocketHandler;
    private final JwtHandshakeInterceptor jwtHandshakeInterceptor;

    public WebSocketConfig(NotificationWebSocketHandler notificationWebSocketHandler,
                          CsWebSocketHandler csWebSocketHandler,
                          JwtHandshakeInterceptor jwtHandshakeInterceptor) {
        this.notificationWebSocketHandler = notificationWebSocketHandler;
        this.csWebSocketHandler = csWebSocketHandler;
        this.jwtHandshakeInterceptor = jwtHandshakeInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(notificationWebSocketHandler, "/api/ws/notifications")
                .addInterceptors(jwtHandshakeInterceptor)
                .setAllowedOrigins("http://localhost:5173", "http://127.0.0.1:5173");
        registry.addHandler(csWebSocketHandler, "/api/ws/cs")
                .addInterceptors(jwtHandshakeInterceptor)
                .setAllowedOrigins("http://localhost:5173", "http://127.0.0.1:5173");
    }
}
