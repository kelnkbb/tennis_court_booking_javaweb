package com.tennis_court_booking.cache;

import com.tennis_court_booking.cache.compensation.BookingCacheConsistencyHandler;
import com.tennis_court_booking.cache.service.CacheEvictCompensationPublisher;
import com.tennis_court_booking.cache.template.RedisLogicalExpireTemplate;
import com.tennis_court_booking.config.CacheExecutorConfig;
import com.tennis_court_booking.pojo.vo.BookingVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 预约详情缓存（Redis L2）：读透传、写后删。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingCacheManager {

    private static final String REDIS_BOOKING_PREFIX = "tennis:cache:booking:vo:";
    private static final String REDIS_BOOKING_REBUILD_LOCK_PREFIX = "tennis:cache:rebuild:lock:booking:";
    private static final RedisLogicalExpireTemplate.CacheOptions BOOKING_CACHE_OPTIONS =
            new RedisLogicalExpireTemplate.CacheOptions(
                    TimeUnit.MINUTES.toSeconds(10),
                    TimeUnit.HOURS.toSeconds(1),
                    TimeUnit.MINUTES.toSeconds(2),
                    20
            );

    private final CacheEvictCompensationPublisher compensationPublisher;
    private final RedisLogicalExpireTemplate redisLogicalExpireTemplate;
    @Qualifier(CacheExecutorConfig.CACHE_TASK_EXECUTOR)
    private final Executor cacheTaskExecutor;

    public BookingVO getBooking(Integer id, Supplier<BookingVO> dbLoader) {
        if (id == null) {
            return null;
        }
        return redisLogicalExpireTemplate.queryObject(
                REDIS_BOOKING_PREFIX + id,
                REDIS_BOOKING_REBUILD_LOCK_PREFIX,
                BookingVO.class,
                dbLoader,
                cacheTaskExecutor,
                BOOKING_CACHE_OPTIONS
        );
    }

    public void evictAfterBookingMutation(Integer bookingId) {
        boolean ok = evictRedisBooking(bookingId);
        if (!ok) {
            compensationPublisher.publishEvictRetry(
                    BookingCacheConsistencyHandler.BIZ_TYPE,
                    bookingId == null ? null : String.valueOf(bookingId),
                    1);
        }
    }

    public boolean evictRedisBooking(Integer bookingId) {
        if (bookingId == null) {
            return true;
        }
        try {
            redisLogicalExpireTemplate.evictKey(REDIS_BOOKING_PREFIX + bookingId);
            return true;
        } catch (Exception e) {
            log.warn("删除预约缓存失败 bookingId={}", bookingId, e);
            return false;
        }
    }
}
