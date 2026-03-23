package com.tennis_court_booking.cache.compensation;

import com.tennis_court_booking.cache.CourtMultilevelCacheManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 场地缓存补偿实现。
 */
@Component
@RequiredArgsConstructor
public class CourtCacheConsistencyHandler implements CacheConsistencyHandler {

    public static final String BIZ_TYPE = "COURT";

    private final CourtMultilevelCacheManager courtCacheManager;

    @Override
    public String bizType() {
        return BIZ_TYPE;
    }

    @Override
    public boolean evictRedis(String bizId) {
        Integer courtId;
        try {
            courtId = bizId == null ? null : Integer.valueOf(bizId);
        } catch (NumberFormatException e) {
            return false;
        }
        return courtCacheManager.evictRedisAfterCourtMutation(courtId);
    }
}
