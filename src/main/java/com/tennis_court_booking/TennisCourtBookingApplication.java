package com.tennis_court_booking;

import dev.langchain4j.community.store.embedding.redis.spring.RedisEmbeddingStoreAutoConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 排除 LangChain4j Redis 向量库的自动配置，避免与 {@code ai.rag} 下显式注册的
 * {@link dev.langchain4j.store.embedding.EmbeddingStore}（内存 / 自建 Redis）产生重复 Bean。
 */
@SpringBootApplication(exclude = {RedisEmbeddingStoreAutoConfiguration.class})
@MapperScan({"com.tennis_court_booking.mapper", "com.tennis_court_booking.coupon.mapper"})
public class TennisCourtBookingApplication {
    public static void main(String[] args) {
        SpringApplication.run(TennisCourtBookingApplication.class, args);
    }
}