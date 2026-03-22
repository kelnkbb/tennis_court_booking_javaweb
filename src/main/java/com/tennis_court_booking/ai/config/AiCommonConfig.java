package com.tennis_court_booking.ai.config;

import com.tennis_court_booking.ai.memory.RedisChatMemoryStore;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class AiCommonConfig {

    @Autowired
    private RedisChatMemoryStore redisChatMemoryStore;

    /**
     * 使用 Redis 实现的 ChatMemoryStore（多实例共享）
     */
    @Bean
    @Primary
    public ChatMemoryStore chatMemoryStore() {
        return redisChatMemoryStore;
    }

    @Bean
    public ChatMemoryProvider chatMemoryProvider(ChatMemoryStore chatMemoryStore) {
        return memoryId -> MessageWindowChatMemory.builder()
                .id(memoryId)
                .maxMessages(20)
                .chatMemoryStore(chatMemoryStore)
                .build();
    }
}