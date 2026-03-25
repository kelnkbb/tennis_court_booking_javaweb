package com.tennis_court_booking.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * WebSocket 全站广播的分布式幂等：多实例或重复调用时，同一逻辑事件只应推送一次。
 * 使用 Redis SET NX + TTL；Redis 异常时放行（fail-open），避免影响主流程。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WsBroadcastIdempotencyService {

    private static final String KEY_PREFIX = "ws:broadcast:idemp:";

    private final StringRedisTemplate stringRedisTemplate;

    @Value("${app.websocket.broadcast-idempotency-ttl-seconds:604800}")
    private long ttlSeconds;

    /**
     * @param broadcastType 业务类型，如 COUPON_ACTIVITY
     * @param uniqueId      业务唯一键，如活动 ID
     * @return true 表示首次占用成功，应执行广播；false 表示近期已广播过，应跳过
     */
    public boolean tryAcquire(String broadcastType, String uniqueId) {
        if (broadcastType == null || broadcastType.isBlank() || uniqueId == null || uniqueId.isBlank()) {
            return true;
        }
        String key = KEY_PREFIX + broadcastType.trim() + ":" + uniqueId.trim();
        try {
            Boolean ok = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", ttlSeconds, TimeUnit.SECONDS);
            if (Boolean.FALSE.equals(ok)) {
                log.info("WS 广播幂等跳过（已存在） key={}", key);
            }
            return Boolean.TRUE.equals(ok);
        } catch (Exception e) {
            log.warn("WS 广播幂等 Redis 异常，放行广播: key={} msg={}", key, e.getMessage());
            return true;
        }
    }
}
