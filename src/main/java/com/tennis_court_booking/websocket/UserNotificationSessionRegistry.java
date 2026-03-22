package com.tennis_court_booking.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 按 userId 维护 WebSocket 会话，支持同用户多标签页。
 */
@Slf4j
@Component
public class UserNotificationSessionRegistry {

    private final ConcurrentHashMap<Integer, Set<WebSocketSession>> userIdToSessions = new ConcurrentHashMap<>();

    public void register(Integer userId, WebSocketSession session) {
        if (userId == null) {
            return;
        }
        userIdToSessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(session);
        log.debug("WS 已注册 userId={} sessionId={}", userId, session.getId());
    }

    public void unregister(WebSocketSession session) {
        Integer userId = (Integer) session.getAttributes().get("userId");
        if (userId == null) {
            return;
        }
        Set<WebSocketSession> set = userIdToSessions.get(userId);
        if (set != null) {
            set.remove(session);
            if (set.isEmpty()) {
                userIdToSessions.remove(userId);
            }
        }
        log.debug("WS 已注销 userId={} sessionId={}", userId, session.getId());
    }

    public void sendJsonToUser(Integer userId, String json) {
        if (userId == null || json == null) {
            return;
        }
        Set<WebSocketSession> set = userIdToSessions.get(userId);
        if (set == null || set.isEmpty()) {
            log.debug("WS 无在线会话 userId={}", userId);
            return;
        }
        TextMessage msg = new TextMessage(json.getBytes(StandardCharsets.UTF_8));
        for (WebSocketSession s : Set.copyOf(set)) {
            if (!s.isOpen()) {
                set.remove(s);
                continue;
            }
            try {
                synchronized (s) {
                    s.sendMessage(msg);
                }
            } catch (IOException e) {
                log.warn("WS 推送失败 userId={} sessionId={}", userId, s.getId(), e);
                try {
                    s.close();
                } catch (IOException ignored) {
                    // ignore
                }
                set.remove(s);
            }
        }
    }
}
