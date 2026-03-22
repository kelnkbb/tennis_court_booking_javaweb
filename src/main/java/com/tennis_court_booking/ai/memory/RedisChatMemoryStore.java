package com.tennis_court_booking.ai.memory;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Redis 持久化对话记忆。必须使用 LangChain4j 自带的 {@link ChatMessageSerializer}/{@link ChatMessageDeserializer}：
 * 工具调用后记忆中会出现 {@code ToolExecutionResultMessage}、带 tool_calls 的 {@code AiMessage} 等，
 * 自定义 Jackson 按 {@code ChatMessage} 注册的序列化器不会覆盖具体子类，会导致 “Failed to serialize chat messages”。
 */
@Slf4j
@Component
public class RedisChatMemoryStore implements ChatMemoryStore {

    private static final String KEY_PREFIX = "chat:memory:";
    private static final long TTL_HOURS = 24;

    private final RedisTemplate<String, String> redisTemplate;

    public RedisChatMemoryStore(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private String buildKey(Object memoryId) {
        return KEY_PREFIX + (memoryId == null ? "null" : memoryId.toString());
    }

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        String key = buildKey(memoryId);
        String json = redisTemplate.opsForValue().get(key);

        if (json == null) {
            log.debug("Redis 中未找到会话历史: {}", key);
            return new ArrayList<>();
        }

        try {
            List<ChatMessage> messages = ChatMessageDeserializer.messagesFromJson(json);
            log.debug("从 Redis 加载会话历史: {}, 消息数: {}", key, messages.size());
            return messages;
        } catch (RuntimeException e) {
            log.warn("反序列化会话历史失败（可能为旧版存储格式），已忽略: {}", key, e);
            try {
                redisTemplate.delete(key);
            } catch (Exception ignored) {
                // ignore
            }
            return new ArrayList<>();
        }
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        String key = buildKey(memoryId);

        if (messages == null || messages.isEmpty()) {
            redisTemplate.delete(key);
            log.debug("删除空会话历史: {}", key);
            return;
        }

        try {
            String json = ChatMessageSerializer.messagesToJson(messages);
            redisTemplate.opsForValue().set(key, json, TTL_HOURS, TimeUnit.HOURS);
            log.debug("保存会话历史到 Redis: {}, 消息数: {}", key, messages.size());
        } catch (RuntimeException e) {
            log.error("序列化会话历史失败: {}", key, e);
            log.error("消息类型概览: {}", summarizeTypes(messages));
            throw new RuntimeException("Failed to serialize chat messages", e);
        }
    }

    private static String summarizeTypes(List<ChatMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < messages.size(); i++) {
            if (i > 0) sb.append(", ");
            ChatMessage m = messages.get(i);
            sb.append(m == null ? "null" : m.getClass().getSimpleName());
        }
        return sb.toString();
    }

    @Override
    public void deleteMessages(Object memoryId) {
        String key = buildKey(memoryId);
        redisTemplate.delete(key);
        log.debug("删除会话历史: {}", key);
    }
}
