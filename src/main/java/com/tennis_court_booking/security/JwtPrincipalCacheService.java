package com.tennis_court_booking.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * JWT 解析通过后，用 Claims 构造的 {@link UserDetails} 放入：
 * <ul>
 *     <li>L1 Caffeine：进程内热点，短 TTL</li>
 *     <li>L2 Redis：多实例共享，减少重复反序列化与对象构建</li>
 * </ul>
 * 全程不查 user 表；权限以 Token 内 role 为准。
 */
@Slf4j
@Service
public class JwtPrincipalCacheService {

    private static final String REDIS_KEY_PREFIX = "tennis:auth:principal:";
    private static final Duration LOCAL_TTL = Duration.ofMinutes(2);
    private static final long REDIS_TTL_MINUTES = 30;

    private final Cache<String, UserDetails> localCache;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public JwtPrincipalCacheService(StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.localCache = Caffeine.newBuilder()
                .maximumSize(4096)
                .expireAfterWrite(LOCAL_TTL)
                .build();
    }

    public UserDetails resolveUserDetails(Claims claims) {
        Integer userId = toInteger(claims.get("userId"));
        Integer role = toInteger(claims.get("role"));
        String username = claims.getSubject();
        String cacheKey = cacheKey(userId, role);

        UserDetails mem = localCache.getIfPresent(cacheKey);
        if (mem != null) {
            return mem;
        }

        String redisKey = REDIS_KEY_PREFIX + cacheKey;
        try {
            String json = stringRedisTemplate.opsForValue().get(redisKey);
            if (json != null) {
                JwtPrincipalSnapshot snap = objectMapper.readValue(json, JwtPrincipalSnapshot.class);
                if (snap.getUsername() != null && snap.getUsername().equals(username)) {
                    UserDetails fromRedis = JwtUserDetailsFactory.fromSnapshot(snap);
                    if (fromRedis != null) {
                        localCache.put(cacheKey, fromRedis);
                        return fromRedis;
                    }
                }
            }
        } catch (JsonProcessingException e) {
            log.debug("Redis 中 principal 反序列化失败，将按 Claims 重建", e);
        }

        UserDetails fresh = JwtUserDetailsFactory.fromClaims(claims);
        localCache.put(cacheKey, fresh);
        try {
            JwtPrincipalSnapshot snap = new JwtPrincipalSnapshot(username, userId, role);
            stringRedisTemplate.opsForValue().set(
                    redisKey, objectMapper.writeValueAsString(snap), REDIS_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (JsonProcessingException e) {
            log.debug("写入 Redis principal 失败", e);
        }
        return fresh;
    }

    private static String cacheKey(Integer userId, Integer role) {
        return (userId == null ? "null" : userId) + ":" + (role == null ? "null" : role);
    }

    private static Integer toInteger(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof Integer) {
            return (Integer) v;
        }
        if (v instanceof Long) {
            return ((Long) v).intValue();
        }
        if (v instanceof Number) {
            return ((Number) v).intValue();
        }
        return null;
    }
}
