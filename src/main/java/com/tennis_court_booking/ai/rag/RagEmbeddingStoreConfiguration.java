package com.tennis_court_booking.ai.rag;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import dev.langchain4j.community.store.embedding.redis.RedisEmbeddingStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 向量存储：默认内存；生产可切换为 Redis Stack 持久化向量索引。
 */
@Configuration
public class RagEmbeddingStoreConfiguration {

    @Bean
    @ConditionalOnProperty(name = "ai.rag.store", havingValue = "memory", matchIfMissing = true)
    public EmbeddingStore<TextSegment> ragEmbeddingStoreMemory() {
        return new InMemoryEmbeddingStore<>();
    }

    @Bean
    @ConditionalOnProperty(name = "ai.rag.store", havingValue = "redis")
    public EmbeddingStore<TextSegment> ragEmbeddingStoreRedis(
            RagProperties ragProperties,
            @Value("${spring.data.redis.host:localhost}") String host,
            @Value("${spring.data.redis.port:6379}") int port,
            @Value("${spring.data.redis.password:}") String password) {

        var builder = RedisEmbeddingStore.builder()
                .host(host)
                .port(port)
                .dimension(ragProperties.getEmbeddingDimension())
                .indexName(ragProperties.getRedisIndexName())
                .metadataKeys(List.of("source"));
        if (StringUtils.hasText(password)) {
            builder.password(password);
        }
        return builder.build();
    }
}
