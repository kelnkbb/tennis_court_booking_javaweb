package com.tennis_court_booking.coupon.async;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * 秒杀抢购专用交换机与队列（与 AI 异步队列隔离）。
 */
@Configuration
public class CouponSeckillRabbitMqConfig {

    public static final String EXCHANGE = "coupon.seckill.exchange";
    public static final String ROUTING_GRAB = "coupon.seckill.grab";
    public static final String QUEUE_GRAB = "coupon.seckill.grab";

    public static final String DLX_EXCHANGE = "coupon.seckill.dlx";
    public static final String DLQ_ROUTING = "coupon.seckill.dlq";
    public static final String QUEUE_DLQ = "coupon.seckill.dlq";

    @Bean
    public DirectExchange couponSeckillExchange() {
        return new DirectExchange(EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange couponSeckillDlxExchange() {
        return new DirectExchange(DLX_EXCHANGE, true, false);
    }

    @Bean
    public Queue couponSeckillGrabQueue() {
        Map<String, Object> args = new HashMap<>(2);
        args.put("x-dead-letter-exchange", DLX_EXCHANGE);
        args.put("x-dead-letter-routing-key", DLQ_ROUTING);
        return QueueBuilder.durable(QUEUE_GRAB).withArguments(args).build();
    }

    @Bean
    public Queue couponSeckillDlq() {
        return QueueBuilder.durable(QUEUE_DLQ).build();
    }

    @Bean
    public Binding couponSeckillGrabBinding() {
        return BindingBuilder.bind(couponSeckillGrabQueue()).to(couponSeckillExchange()).with(ROUTING_GRAB);
    }

    @Bean
    public Binding couponSeckillDlqBinding() {
        return BindingBuilder.bind(couponSeckillDlq()).to(couponSeckillDlxExchange()).with(DLQ_ROUTING);
    }
}
