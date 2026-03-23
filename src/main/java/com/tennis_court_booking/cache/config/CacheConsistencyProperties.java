package com.tennis_court_booking.cache.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 缓存一致性补偿配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "cache.consistency")
public class CacheConsistencyProperties {

    /** 补偿最大重试次数（含首次补偿消息）。 */
    private int maxAttempts = 5;
}
