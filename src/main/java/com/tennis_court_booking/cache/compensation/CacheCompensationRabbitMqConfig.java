package com.tennis_court_booking.cache.compensation;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 缓存一致性补偿队列配置。
 */
@Configuration
public class CacheCompensationRabbitMqConfig {

    public static final String EXCHANGE = "cache.consistency.exchange";
    public static final String ROUTING_EVICT_RETRY = "cache.consistency.evict.retry";
    public static final String QUEUE_EVICT_RETRY = "cache.consistency.evict.retry";

    @Bean
    public DirectExchange cacheConsistencyExchange() {
        return new DirectExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue cacheEvictRetryQueue() {
        return QueueBuilder.durable(QUEUE_EVICT_RETRY).build();
    }

    @Bean
    public Binding cacheEvictRetryBinding() {
        return BindingBuilder.bind(cacheEvictRetryQueue()).to(cacheConsistencyExchange()).with(ROUTING_EVICT_RETRY);
    }
}
