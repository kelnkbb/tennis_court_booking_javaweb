package com.tennis_court_booking.cs.service;

import com.tennis_court_booking.cs.entity.CsConversation;
import com.tennis_court_booking.cs.entity.CsMessage;
import com.tennis_court_booking.cs.mapper.CsConversationMapper;
import com.tennis_court_booking.cs.mapper.CsMessageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CsChatService {

    public static final int ROLE_ADMIN = 2;
    public static final int SENDER_USER = 1;
    public static final int SENDER_STAFF = 2;
    public static final int CONV_OPEN = 1;
    public static final int CONV_CLOSED = 0;

    private static final int MAX_CONTENT_LEN = 4000;

    private final CsConversationMapper conversationMapper;
    private final CsMessageMapper messageMapper;

    /**
     * 普通用户：获取当前进行中的会话；若无则新建。
     */
    @Transactional
    public CsConversation getOrCreateOpenConversation(Integer userId) {
        CsConversation existing = conversationMapper.findOpenByUserId(userId);
        if (existing != null) {
            return existing;
        }
        LocalDateTime now = LocalDateTime.now();
        CsConversation row = new CsConversation(null, userId, CONV_OPEN, now, now);
        conversationMapper.insert(row);
        return conversationMapper.findById(row.getId());
    }

    public CsConversation findConversation(Long id) {
        return id == null ? null : conversationMapper.findById(id);
    }

    /** 当前用户进行中的会话（无则返回 null，不自动创建）。 */
    public CsConversation getOpenConversationForUser(Integer userId) {
        return conversationMapper.findOpenByUserId(userId);
    }

    /**
     * 校验用户是否可访问该会话（本人或管理员）。
     */
    public boolean canAccess(Integer userId, Integer role, CsConversation conv) {
        if (conv == null || userId == null) {
            return false;
        }
        if (role != null && role == ROLE_ADMIN) {
            return true;
        }
        return conv.getUserId().equals(userId);
    }

    /**
     * 落库并返回实体（含 id）。
     */
    @Transactional
    public CsMessage appendMessage(Long conversationId, int senderType, Integer senderUserId, String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("内容不能为空");
        }
        String trimmed = content.trim();
        if (trimmed.length() > MAX_CONTENT_LEN) {
            throw new IllegalArgumentException("内容过长");
        }
        LocalDateTime now = LocalDateTime.now();
        CsMessage m = new CsMessage(null, conversationId, senderType, senderUserId, trimmed, now);
        messageMapper.insert(m);
        conversationMapper.updateUpdatedAt(conversationId, now);
        return m;
    }

    public List<CsMessage> listRecentMessagesChronological(Long conversationId, int limit) {
        if (conversationId == null || limit <= 0) {
            return Collections.emptyList();
        }
        int cap = Math.min(limit, 500);
        List<CsMessage> rows = messageMapper.listByConversationIdDesc(conversationId, 0, cap);
        Collections.reverse(rows);
        return rows;
    }

    public List<CsConversation> listOpenConversations(int limit) {
        int cap = Math.min(Math.max(limit, 1), 200);
        return conversationMapper.listOpen(cap);
    }

    @Transactional
    public void closeConversationForUser(Long conversationId, Integer userId) {
        CsConversation conv = conversationMapper.findById(conversationId);
        if (conv == null || !conv.getUserId().equals(userId)) {
            throw new IllegalArgumentException("会话不存在或无权操作");
        }
        if (conv.getStatus() != CONV_OPEN) {
            return;
        }
        conversationMapper.closeById(conversationId, LocalDateTime.now());
    }

    @Transactional
    public void closeConversationForAdmin(Long conversationId) {
        CsConversation conv = conversationMapper.findById(conversationId);
        if (conv == null) {
            throw new IllegalArgumentException("会话不存在");
        }
        if (conv.getStatus() != CONV_OPEN) {
            return;
        }
        conversationMapper.closeById(conversationId, LocalDateTime.now());
    }
}
