package com.tennis_court_booking.cache.service;

import com.tennis_court_booking.cache.compensation.CacheCompensationRabbitMqConfig;
import com.tennis_court_booking.cache.compensation.CacheEvictCompensationMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

/**
 * 发布缓存删除补偿消息。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CacheEvictCompensationPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishEvictRetry(String bizType, String bizId, int attempt) {
        CacheEvictCompensationMessage msg = new CacheEvictCompensationMessage(bizType, bizId, attempt);
        rabbitTemplate.convertAndSend(
                CacheCompensationRabbitMqConfig.EXCHANGE,
                CacheCompensationRabbitMqConfig.ROUTING_EVICT_RETRY,
                msg);
        log.warn("已投递缓存删除补偿消息 bizType={} bizId={} attempt={}", bizType, bizId, attempt);
    }
}
