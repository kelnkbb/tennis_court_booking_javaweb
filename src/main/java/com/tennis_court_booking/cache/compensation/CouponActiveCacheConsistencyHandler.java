package com.tennis_court_booking.cache.compensation;

import com.tennis_court_booking.cache.CouponActivityCacheManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 秒杀活动进行中列表缓存补偿实现。
 */
@Component
@RequiredArgsConstructor
public class CouponActiveCacheConsistencyHandler implements CacheConsistencyHandler {

    public static final String BIZ_TYPE = "COUPON_ACTIVE";

    private final CouponActivityCacheManager couponActivityCacheManager;

    @Override
    public String bizType() {
        return BIZ_TYPE;
    }

    @Override
    public boolean evictRedis(String bizId) {
        return couponActivityCacheManager.evictRedisActiveActivities();
    }
}
