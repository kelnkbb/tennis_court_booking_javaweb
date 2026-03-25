package com.tennis_court_booking.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.tennis_court_booking.cache.compensation.UserCacheConsistencyHandler;
import com.tennis_court_booking.cache.service.CacheEvictCompensationPublisher;
import com.tennis_court_booking.cache.template.RedisLogicalExpireTemplate;
import com.tennis_court_booking.config.CacheExecutorConfig;
import com.tennis_court_booking.pojo.entity.User;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 用户信息多级缓存（Caffeine L1 + Redis L2）。
 * 缓存与 Redis 中均不保存密码；登录/凭证校验请走数据库（如 {@code findByUsername}、{@code findByIdWithPassword}）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserCacheManager {

    private static final String REDIS_USER_PREFIX = "tennis:cache:user:";
    private static final String REDIS_USER_REBUILD_LOCK_PREFIX = "tennis:cache:rebuild:lock:user:";
    private static final RedisLogicalExpireTemplate.CacheOptions USER_CACHE_OPTIONS =
            new RedisLogicalExpireTemplate.CacheOptions(
                    TimeUnit.MINUTES.toSeconds(10),
                    TimeUnit.HOURS.toSeconds(1),
                    TimeUnit.MINUTES.toSeconds(2),
                    20
            );

    private static final Duration LOCAL_TTL = Duration.ofMinutes(5);
    private static final long LOCAL_MAX_ENTRIES = 4096;

    private final CacheEvictCompensationPublisher compensationPublisher;
    private final RedisLogicalExpireTemplate redisLogicalExpireTemplate;
    @Qualifier(CacheExecutorConfig.CACHE_TASK_EXECUTOR)
    private final Executor cacheTaskExecutor;

    private Cache<Integer, User> localById;

    @PostConstruct
    void initLocal() {
        this.localById = Caffeine.newBuilder()
                .maximumSize(LOCAL_MAX_ENTRIES)
                .expireAfterWrite(LOCAL_TTL)
                .build();
    }

    public User getUserById(Integer id, Supplier<User> dbLoader) {
        if (id == null) {
            return null;
        }
        User hit = localById.getIfPresent(id);
        if (hit != null) {
            log.debug("[UserCache] getUserById id={} -> L1_Caffeine_HIT", id);
            return hit;
        }
        Supplier<User> sanitizingLoader = () -> sanitizeForCache(dbLoader.get());
        User from = redisLogicalExpireTemplate.queryObject(
                REDIS_USER_PREFIX + id,
                REDIS_USER_REBUILD_LOCK_PREFIX,
                User.class,
                sanitizingLoader,
                cacheTaskExecutor,
                USER_CACHE_OPTIONS
        );
        if (from != null) {
            localById.put(id, from);
        }
        return from;
    }

    public void evictAfterUserMutation(Integer userId) {
        if (userId != null) {
            localById.invalidate(userId);
        }
        boolean ok = evictRedisUser(userId);
        if (!ok) {
            compensationPublisher.publishEvictRetry(
                    UserCacheConsistencyHandler.BIZ_TYPE,
                    userId == null ? null : String.valueOf(userId),
                    1);
        }
    }

    public boolean evictRedisUser(Integer userId) {
        if (userId == null) {
            return true;
        }
        try {
            redisLogicalExpireTemplate.evictKey(REDIS_USER_PREFIX + userId);
            return true;
        } catch (Exception e) {
            log.warn("删除用户缓存失败 userId={}", userId, e);
            return false;
        }
    }

    static User sanitizeForCache(User raw) {
        if (raw == null) {
            return null;
        }
        User c = new User();
        c.setId(raw.getId());
        c.setUsername(raw.getUsername());
        c.setPassword(null);
        c.setPhone(raw.getPhone());
        c.setEmail(raw.getEmail());
        c.setRealName(raw.getRealName());
        c.setRole(raw.getRole());
        c.setStatus(raw.getStatus());
        c.setCreateTime(raw.getCreateTime());
        c.setUpdateTime(raw.getUpdateTime());
        return c;
    }
}
