package com.tennis_court_booking.cs.ws;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tennis_court_booking.cs.entity.CsConversation;
import com.tennis_court_booking.cs.entity.CsMessage;
import com.tennis_court_booking.cs.service.CsChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * 人工客服双向通道：用户连接自动绑定当前进行中的会话；管理员连接需带 conversationId。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CsWebSocketHandler extends TextWebSocketHandler {

    private final CsChatService csChatService;
    private final CsSessionRegistry csSessionRegistry;
    private final ObjectMapper objectMapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Integer userId = (Integer) session.getAttributes().get("userId");
        Integer role = (Integer) session.getAttributes().get("role");
        if (userId == null) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("未认证"));
            return;
        }

        boolean isAdmin = role != null && role == CsChatService.ROLE_ADMIN;
        if (isAdmin) {
            Long cid = parseConversationId(session.getUri());
            if (cid == null) {
                session.close(CloseStatus.NOT_ACCEPTABLE.withReason("管理员请使用 ?conversationId="));
                return;
            }
            CsConversation conv = csChatService.findConversation(cid);
            if (conv == null) {
                session.close(CloseStatus.NOT_ACCEPTABLE.withReason("会话不存在"));
                return;
            }
            session.getAttributes().put("csRole", "admin");
            csSessionRegistry.register(cid, session);
            sendJson(session, joinedPayload(cid, "admin"));
            return;
        }

        session.getAttributes().put("csRole", "user");
        CsConversation conv = csChatService.getOrCreateOpenConversation(userId);
        csSessionRegistry.register(conv.getId(), session);
        sendJson(session, joinedPayload(conv.getId(), "user"));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String p = message.getPayload();
        if ("ping".equalsIgnoreCase(p)) {
            session.sendMessage(new TextMessage("pong"));
            return;
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(p);
        } catch (Exception e) {
            sendError(session, "消息格式应为 JSON");
            return;
        }

        String type = root.path("type").asText("");
        if (!"chat".equals(type)) {
            sendError(session, "不支持的 type");
            return;
        }

        String content = root.path("content").asText(null);
        Long conversationId = (Long) session.getAttributes().get("csConversationId");
        Integer userId = (Integer) session.getAttributes().get("userId");
        Integer role = (Integer) session.getAttributes().get("role");
        String csRole = (String) session.getAttributes().get("csRole");

        if (conversationId == null || userId == null) {
            sendError(session, "会话未就绪");
            return;
        }

        CsConversation conv = csChatService.findConversation(conversationId);
        if (conv == null) {
            sendError(session, "会话不存在");
            return;
        }
        if (conv.getStatus() != CsChatService.CONV_OPEN) {
            sendError(session, "会话已结束");
            return;
        }

        boolean isAdmin = "admin".equals(csRole);
        if (!csChatService.canAccess(userId, role, conv)) {
            sendError(session, "无权在此会话发言");
            return;
        }

        int senderType = isAdmin ? CsChatService.SENDER_STAFF : CsChatService.SENDER_USER;
        CsMessage saved;
        try {
            saved = csChatService.appendMessage(conversationId, senderType, userId, content);
        } catch (IllegalArgumentException ex) {
            sendError(session, ex.getMessage());
            return;
        }

        ObjectNode out = objectMapper.createObjectNode();
        out.put("type", "message");
        out.put("id", saved.getId());
        out.put("conversationId", saved.getConversationId());
        out.put("senderType", saved.getSenderType());
        out.put("senderUserId", saved.getSenderUserId());
        out.put("content", saved.getContent());
        out.put("createdAt", saved.getCreatedAt().toString());

        String json = objectMapper.writeValueAsString(out);
        csSessionRegistry.broadcastJson(conversationId, json);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        csSessionRegistry.unregister(session);
    }

    private static Long parseConversationId(URI uri) {
        if (uri == null || uri.getQuery() == null) {
            return null;
        }
        for (String part : uri.getQuery().split("&")) {
            int eq = part.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            String key = part.substring(0, eq);
            if (!"conversationId".equals(key)) {
                continue;
            }
            String raw = part.substring(eq + 1);
            String v = URLDecoder.decode(raw, StandardCharsets.UTF_8);
            try {
                return Long.parseLong(v);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private ObjectNode joinedPayload(Long conversationId, String role) {
        ObjectNode n = objectMapper.createObjectNode();
        n.put("type", "joined");
        n.put("conversationId", conversationId);
        n.put("role", role);
        return n;
    }

    private void sendJson(WebSocketSession session, ObjectNode node) throws IOException {
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(node)));
    }

    private void sendError(WebSocketSession session, String msg) throws IOException {
        ObjectNode n = objectMapper.createObjectNode();
        n.put("type", "error");
        n.put("message", msg);
        sendJson(session, n);
    }
}
