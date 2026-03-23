package com.tennis_court_booking.cache.compensation;

import com.tennis_court_booking.cache.BookingCacheManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 预约缓存补偿实现。
 */
@Component
@RequiredArgsConstructor
public class BookingCacheConsistencyHandler implements CacheConsistencyHandler {

    public static final String BIZ_TYPE = "BOOKING";

    private final BookingCacheManager bookingCacheManager;

    @Override
    public String bizType() {
        return BIZ_TYPE;
    }

    @Override
    public boolean evictRedis(String bizId) {
        Integer bookingId;
        try {
            bookingId = bizId == null ? null : Integer.valueOf(bizId);
        } catch (NumberFormatException e) {
            return false;
        }
        return bookingCacheManager.evictRedisBooking(bookingId);
    }
}
