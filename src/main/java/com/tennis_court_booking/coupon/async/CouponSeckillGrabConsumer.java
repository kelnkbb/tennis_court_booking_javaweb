package com.tennis_court_booking.coupon.async;

import com.tennis_court_booking.coupon.config.CouponSeckillListenerContainerFactoryConfig;
import com.tennis_court_booking.coupon.service.CouponSeckillService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 秒杀抢购消费者：异步执行 Redis Lua 库存扣减与券记录生成。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CouponSeckillGrabConsumer {

    private final CouponSeckillService couponSeckillService;

    @RabbitListener(
            queues = CouponSeckillRabbitMqConfig.QUEUE_GRAB,
            containerFactory = CouponSeckillListenerContainerFactoryConfig.BEAN_NAME)
    public void onGrab(SeckillGrabMessage message) {
        if (message == null || message.getGrabId() == null) {
            log.warn("忽略空秒杀消息");
            return;
        }
        log.debug("处理秒杀消息 grabId={} activityId={} userId={}", message.getGrabId(), message.getActivityId(), message.getUserId());
        couponSeckillService.processGrabFromQueue(message);
    }
}
