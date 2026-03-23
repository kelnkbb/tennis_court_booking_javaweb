package com.tennis_court_booking.coupon.async;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 秒杀抢购异步消息：由 API 入队，消费者执行 Redis 扣减与券记录落库。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeckillGrabMessage implements Serializable {

    private String grabId;
    private Integer activityId;
    private Integer userId;
}
