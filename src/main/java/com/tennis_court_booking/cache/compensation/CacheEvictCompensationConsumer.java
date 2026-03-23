package com.tennis_court_booking.cache.compensation;

import com.tennis_court_booking.cache.config.CacheConsistencyProperties;
import com.tennis_court_booking.cache.service.CacheEvictCompensationPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 统一缓存删除补偿消费者：按 bizType 分发给不同 handler，失败后递增 attempt 再入队，直到 maxAttempts。
 */
@Slf4j
@Component
public class CacheEvictCompensationConsumer {

    private final CacheEvictCompensationPublisher publisher;
    private final CacheConsistencyProperties properties;
    private final Map<String, CacheConsistencyHandler> handlers;

    public CacheEvictCompensationConsumer(
            CacheEvictCompensationPublisher publisher,
            CacheConsistencyProperties properties,
            List<CacheConsistencyHandler> handlers) {
        this.publisher = publisher;
        this.properties = properties;
        this.handlers = handlers.stream().collect(Collectors.toMap(CacheConsistencyHandler::bizType, Function.identity()));
    }

    @RabbitListener(queues = CacheCompensationRabbitMqConfig.QUEUE_EVICT_RETRY)
    public void onMessage(CacheEvictCompensationMessage msg) {
        if (msg == null || msg.getBizType() == null) {
            return;
        }
        CacheConsistencyHandler handler = handlers.get(msg.getBizType());
        if (handler == null) {
            log.warn("未知补偿消息类型 bizType={}", msg.getBizType());
            return;
        }
        String bizId = msg.getBizId();
        int attempt = Math.max(msg.getAttempt(), 1);
        boolean ok = handler.evictRedis(bizId);
        if (ok) {
            log.info("缓存删除补偿成功 bizType={} bizId={} attempt={}", msg.getBizType(), bizId, attempt);
            return;
        }
        int max = Math.max(properties.getMaxAttempts(), 1);
        if (attempt >= max) {
            log.error("缓存删除补偿到达上限仍失败 bizType={} bizId={} maxAttempts={}", msg.getBizType(), bizId, max);
            return;
        }
        publisher.publishEvictRetry(msg.getBizType(), bizId, attempt + 1);
    }
}
