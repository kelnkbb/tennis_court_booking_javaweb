package com.tennis_court_booking.cache.compensation;

import com.tennis_court_booking.cache.UserCacheManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 用户缓存删除补偿。
 */
@Component
@RequiredArgsConstructor
public class UserCacheConsistencyHandler implements CacheConsistencyHandler {

    public static final String BIZ_TYPE = "USER";

    private final UserCacheManager userCacheManager;

    @Override
    public String bizType() {
        return BIZ_TYPE;
    }

    @Override
    public boolean evictRedis(String bizId) {
        Integer userId;
        try {
            userId = bizId == null ? null : Integer.valueOf(bizId);
        } catch (NumberFormatException e) {
            return false;
        }
        return userCacheManager.evictRedisUser(userId);
    }
}
