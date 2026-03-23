package com.tennis_court_booking.coupon.service;

import com.tennis_court_booking.coupon.async.CouponSeckillRabbitMqConfig;
import com.tennis_court_booking.coupon.async.SeckillGrabMessage;
import com.tennis_court_booking.coupon.async.SeckillGrabRedisResult;
import com.tennis_court_booking.coupon.mapper.CouponActivityMapper;
import com.tennis_court_booking.coupon.pojo.CouponActivity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * 秒杀抢购异步入口：校验活动后写入 PENDING 结果并入队，由消费者执行 Redis 扣减与落库。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CouponSeckillAsyncService {

    private final CouponActivityMapper couponActivityMapper;
    private final CouponSeckillService couponSeckillService;
    private final RabbitTemplate rabbitTemplate;

    /**
     * @return grabId，客户端轮询 {@code GET /api/coupons/seckill/result/{grabId}}
     */
    public String submitGrab(Integer activityId, Integer userId) {
        CouponActivity act = couponActivityMapper.findById(activityId);
        CouponSeckillService.SeckillGrabResult err = couponSeckillService.validateActivityForGrab(act);
        if (err != null) {
            throw new IllegalArgumentException(err.message());
        }

        String grabId = UUID.randomUUID().toString().replace("-", "");
        SeckillGrabRedisResult pending = SeckillGrabRedisResult.pending(activityId, userId);
        couponSeckillService.writeGrabResult(grabId, pending);

        try {
            rabbitTemplate.convertAndSend(
                    CouponSeckillRabbitMqConfig.EXCHANGE,
                    CouponSeckillRabbitMqConfig.ROUTING_GRAB,
                    new SeckillGrabMessage(grabId, activityId, userId));
        } catch (Exception e) {
            log.error("秒杀消息入队失败 grabId={} activityId={} userId={}", grabId, activityId, userId, e);
            couponSeckillService.deleteGrabResult(grabId);
            throw new IllegalStateException("秒杀服务繁忙，请稍后重试", e);
        }
        return grabId;
    }
}
