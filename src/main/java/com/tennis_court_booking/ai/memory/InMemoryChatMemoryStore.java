package com.tennis_court_booking.ai.memory;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 简单的内存版会话存储（开发/演示用）。
 * 若要多实例部署，需要替换为 Redis/Mongo 等持久化存储。
 */
public class InMemoryChatMemoryStore implements ChatMemoryStore {

    private final Map<String, List<ChatMessage>> store = new ConcurrentHashMap<>();

    private String key(Object memoryId) {
        return memoryId == null ? "null" : memoryId.toString();
    }

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        return store.getOrDefault(key(memoryId), new ArrayList<>());
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> list) {
        store.put(key(memoryId), list == null ? new ArrayList<>() : new ArrayList<>(list));
    }

    @Override
    public void deleteMessages(Object memoryId) {
        store.remove(key(memoryId));
    }
}

