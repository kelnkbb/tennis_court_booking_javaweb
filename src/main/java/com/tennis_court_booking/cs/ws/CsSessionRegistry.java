package com.tennis_court_booking.cs.ws;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 按会话 ID 维护人工客服 WebSocket，同一会话内用户与管理员多端可同时在线。
 */
@Slf4j
@Component
public class CsSessionRegistry {

    private final ConcurrentHashMap<Long, Set<WebSocketSession>> conversationIdToSessions = new ConcurrentHashMap<>();

    public void register(Long conversationId, WebSocketSession session) {
        if (conversationId == null) {
            return;
        }
        session.getAttributes().put("csConversationId", conversationId);
        conversationIdToSessions.computeIfAbsent(conversationId, k -> ConcurrentHashMap.newKeySet()).add(session);
        log.debug("CS WS 已注册 conversationId={} sessionId={}", conversationId, session.getId());
    }

    public void unregister(WebSocketSession session) {
        Long conversationId = (Long) session.getAttributes().get("csConversationId");
        if (conversationId == null) {
            return;
        }
        Set<WebSocketSession> set = conversationIdToSessions.get(conversationId);
        if (set != null) {
            set.remove(session);
            if (set.isEmpty()) {
                conversationIdToSessions.remove(conversationId);
            }
        }
        log.debug("CS WS 已注销 conversationId={} sessionId={}", conversationId, session.getId());
    }

    public void broadcastJson(Long conversationId, String json) {
        if (conversationId == null || json == null) {
            return;
        }
        Set<WebSocketSession> set = conversationIdToSessions.get(conversationId);
        if (set == null || set.isEmpty()) {
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
                log.warn("CS WS 推送失败 conversationId={} sessionId={}", conversationId, s.getId(), e);
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
