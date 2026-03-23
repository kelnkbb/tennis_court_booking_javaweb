package com.tennis_court_booking.limit.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Redis 滑动窗口限流：
 * 采用 ZSET 存储时间戳，执行 Lua：
 * - 移除窗口外记录
 * - 统计窗口内数量
 * - 允许则新增当前时间戳
 */
@Slf4j
@Service
public class RedisSlidingWindowRateLimiter implements SlidingWindowRateLimiter {

    private final StringRedisTemplate stringRedisTemplate;

    private final DefaultRedisScript<Long> script;

    public RedisSlidingWindowRateLimiter(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
        DefaultRedisScript<Long> s = new DefaultRedisScript<>();
        // 返回 1=允许，0=限流
        s.setScriptText(LUA_SLIDING_WINDOW);
        s.setResultType(Long.class);
        this.script = s;
    }

    private static final String LUA_SLIDING_WINDOW = """
            local key = KEYS[1]
            local now = tonumber(ARGV[1])
            local windowMs = tonumber(ARGV[2])
            local maxRequests = tonumber(ARGV[3])
            local reqId = ARGV[4]
            -- 过期时间：now - windowMs
            local minScore = now - windowMs
            redis.call('ZREMRANGEBYSCORE', key, 0, minScore)
            local count = redis.call('ZCARD', key)
            if count < maxRequests then
              redis.call('ZADD', key, now, tostring(reqId))
              redis.call('PEXPIRE', key, windowMs)
              return 1
            end
            return 0
            """;

    @Override
    public boolean tryAcquire(String limitKey, int windowSeconds, int maxRequests) {
        if (limitKey == null || limitKey.isBlank()) {
            return true;
        }
        long nowMs = System.currentTimeMillis();
        long windowMs = windowSeconds * 1000L;
        String reqId = String.valueOf(System.nanoTime());
        Long r = stringRedisTemplate.execute(
                script,
                List.of(limitKey),
                String.valueOf(nowMs),
                String.valueOf(windowMs),
                String.valueOf(maxRequests),
                reqId
        );
        return r != null && r == 1L;
    }
}

