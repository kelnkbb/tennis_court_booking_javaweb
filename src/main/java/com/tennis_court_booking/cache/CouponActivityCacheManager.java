package com.tennis_court_booking.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.tennis_court_booking.cache.compensation.CouponActiveCacheConsistencyHandler;
import com.tennis_court_booking.cache.service.CacheEvictCompensationPublisher;
import com.tennis_court_booking.cache.template.RedisLogicalExpireTemplate;
import com.tennis_court_booking.config.CacheExecutorConfig;
import com.tennis_court_booking.coupon.pojo.CouponActivity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 秒杀活动列表缓存（进行中列表）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CouponActivityCacheManager {

    private static final String REDIS_ACTIVE_LIST = "tennis:cache:coupon:activities:active";
    private static final String REDIS_ACTIVE_LIST_REBUILD_LOCK_PREFIX = "tennis:cache:rebuild:lock:coupon:active:";
    private static final RedisLogicalExpireTemplate.CacheOptions ACTIVE_LIST_CACHE_OPTIONS =
            new RedisLogicalExpireTemplate.CacheOptions(
                    TimeUnit.MINUTES.toSeconds(3),
                    TimeUnit.MINUTES.toSeconds(30),
                    TimeUnit.MINUTES.toSeconds(1),
                    15
            );
    private static final String ACTIVE_BIZ_ID = "ACTIVE";

    private final CacheEvictCompensationPublisher compensationPublisher;
    private final RedisLogicalExpireTemplate redisLogicalExpireTemplate;
    @Qualifier(CacheExecutorConfig.CACHE_TASK_EXECUTOR)
    private final Executor cacheTaskExecutor;

    public List<CouponActivity> getActiveActivities(Supplier<List<CouponActivity>> dbLoader) {
        List<CouponActivity> list = redisLogicalExpireTemplate.queryWithTypeRef(
                REDIS_ACTIVE_LIST,
                REDIS_ACTIVE_LIST_REBUILD_LOCK_PREFIX,
                new TypeReference<List<CouponActivity>>() {},
                dbLoader,
                cacheTaskExecutor,
                ACTIVE_LIST_CACHE_OPTIONS
        );
        return list == null ? Collections.emptyList() : list;
    }

    public void evictAfterActivityMutation() {
        boolean ok = evictRedisActiveActivities();
        if (!ok) {
            compensationPublisher.publishEvictRetry(
                    CouponActiveCacheConsistencyHandler.BIZ_TYPE,
                    ACTIVE_BIZ_ID,
                    1);
        }
    }

    public boolean evictRedisActiveActivities() {
        try {
            redisLogicalExpireTemplate.evictKey(REDIS_ACTIVE_LIST);
            return true;
        } catch (Exception e) {
            log.warn("删除秒杀活动列表缓存失败", e);
            return false;
        }
    }
}
