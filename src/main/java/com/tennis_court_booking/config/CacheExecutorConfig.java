package com.tennis_court_booking.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 缓存热度统计、ZSet 裁剪、热点 Set 刷新等异步任务专用线程池，避免占用 Tomcat 工作线程。
 */
@Configuration
public class CacheExecutorConfig {

    public static final String CACHE_TASK_EXECUTOR = "cacheTaskExecutor";

    @Bean(name = CACHE_TASK_EXECUTOR)
    public Executor cacheTaskExecutor() {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(2);
        ex.setMaxPoolSize(8);
        ex.setQueueCapacity(500);
        ex.setThreadNamePrefix("cache-async-");
        ex.initialize();
        return ex;
    }
}
