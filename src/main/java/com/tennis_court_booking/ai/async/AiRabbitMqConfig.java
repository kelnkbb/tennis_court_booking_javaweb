package com.tennis_court_booking.ai.async;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class AiRabbitMqConfig {

    public static final String EXCHANGE = "ai.chat.exchange";
    public static final String ROUTING_WORK = "ai.chat.work";
    public static final String QUEUE_WORK = "ai.chat.work";

    public static final String DLX_EXCHANGE = "ai.chat.dlx";
    public static final String DLQ_ROUTING = "ai.chat.dlq";
    public static final String QUEUE_DLQ = "ai.chat.dlq";

    @Bean
    public DirectExchange aiChatExchange() {
        return new DirectExchange(EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange aiChatDlxExchange() {
        return new DirectExchange(DLX_EXCHANGE, true, false);
    }

    @Bean
    public Queue aiChatWorkQueue() {
        Map<String, Object> args = new HashMap<>(2);
        args.put("x-dead-letter-exchange", DLX_EXCHANGE);
        args.put("x-dead-letter-routing-key", DLQ_ROUTING);
        return QueueBuilder.durable(QUEUE_WORK).withArguments(args).build();
    }

    @Bean
    public Queue aiChatDlq() {
        return QueueBuilder.durable(QUEUE_DLQ).build();
    }

    @Bean
    public Binding aiChatWorkBinding() {
        return BindingBuilder.bind(aiChatWorkQueue()).to(aiChatExchange()).with(ROUTING_WORK);
    }

    @Bean
    public Binding aiChatDlqBinding() {
        return BindingBuilder.bind(aiChatDlq()).to(aiChatDlxExchange()).with(DLQ_ROUTING);
    }

    /**
     * JSON 消息转换器（移除 setAllowedListPatterns 配置）
     */
    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter(ObjectMapper objectMapper) {
        // 直接创建转换器，不设置白名单（老版本兼容）
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}