package com.tennis_court_booking.coupon.config;

import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 秒杀消费者专用：不使用全局 listener 重试（避免 Lua 与 DB 状态在重复投递时歧义），由业务内自行回滚 Redis。
 */
@Configuration
public class CouponSeckillListenerContainerFactoryConfig {

    public static final String BEAN_NAME = "couponSeckillListenerContainerFactory";

    @Bean(name = BEAN_NAME)
    public SimpleRabbitListenerContainerFactory couponSeckillListenerContainerFactory(
            ConnectionFactory connectionFactory,
            @Qualifier("jackson2JsonMessageConverter") Jackson2JsonMessageConverter messageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        factory.setConcurrentConsumers(4);
        factory.setMaxConcurrentConsumers(16);
        factory.setPrefetchCount(50);
        factory.setDefaultRequeueRejected(false);
        return factory;
    }
}
