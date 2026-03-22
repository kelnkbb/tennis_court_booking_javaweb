package com.tennis_court_booking.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.tennis_court_booking.config.CacheExecutorConfig;
import com.tennis_court_booking.pojo.entity.Court;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 场地读路径多级缓存（Caffeine L1 + Redis L2）与热点追踪（Redis ZSet + Set）。
 * 写路径由业务在改库后调用 {@link #evictAfterCourtMutation(Integer)}。
 */
@Slf4j
@Component
public class CourtMultilevelCacheManager {

    private static final String REDIS_COURT_PREFIX = "tennis:cache:court:";
    private static final String REDIS_LIST_ALL = "tennis:cache:court:list:all";
    /** ZSet：member=场地 id，score=访问热度 */
    private static final String REDIS_ZSET_HEAT = "tennis:hot:court:heat";
    /** Set：当前 TopN 热点 id，便于 O(1) 判断或展示 */
    private static final String REDIS_SET_HOT_IDS = "tennis:hot:court:ids";

    private static final Duration LOCAL_TTL = Duration.ofMinutes(5);
    private static final long REDIS_TTL_SECONDS = TimeUnit.MINUTES.toSeconds(10);
    private static final int LOCAL_MAX_SINGLE_COURTS = 512;
    /** ZSet 超过该规模则裁剪掉低分 member，只保留 HOT_KEEP 个 */
    private static final long ZSET_TRIM_THRESHOLD = 200;
    private static final long HOT_KEEP = 100;
    private static final int HOT_TOP_FOR_SET = 32;

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final Executor cacheTaskExecutor;

    private Cache<Integer, Court> localById;
    private Cache<String, List<Court>> localListAll;

    public CourtMultilevelCacheManager(
            StringRedisTemplate stringRedisTemplate,
            ObjectMapper objectMapper,
            @Qualifier(CacheExecutorConfig.CACHE_TASK_EXECUTOR) Executor cacheTaskExecutor) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.cacheTaskExecutor = cacheTaskExecutor;
    }

    @PostConstruct
    void initLocal() {
        this.localById = Caffeine.newBuilder()
                .maximumSize(LOCAL_MAX_SINGLE_COURTS)
                .expireAfterWrite(LOCAL_TTL)
                .build();
        this.localListAll = Caffeine.newBuilder()
                .maximumSize(4)
                .expireAfterWrite(LOCAL_TTL)
                .build();
    }

    public Court getCourt(Integer id, Supplier<Court> dbLoader) {
        if (id == null) {
            return null;
        }
        Court hit = localById.getIfPresent(id);
        if (hit != null) {
            log.debug("[CourtCache] getCourt id={} -> L1_Caffeine_HIT", id);
            recordHeatAsync(id);
            return hit;
        }
        String json = stringRedisTemplate.opsForValue().get(REDIS_COURT_PREFIX + id);
        if (json != null) {
            Court c = readCourt(json);
            if (c != null) {
                log.debug("[CourtCache] getCourt id={} -> L2_Redis_HIT，回写L1", id);
                localById.put(id, c);
                recordHeatAsync(id);
                return c;
            }
        }
        log.debug("[CourtCache] getCourt id={} -> DB_LOAD", id);
        Court fromDb = dbLoader.get();
        if (fromDb != null) {
            writeCourtToRedis(id, fromDb);
            localById.put(id, fromDb);
            recordHeatAsync(id);
        }
        return fromDb;
    }

    public List<Court> findAllCourts(Supplier<List<Court>> dbLoader) {
        final String lk = "all";
        List<Court> mem = localListAll.getIfPresent(lk);
        if (mem != null) {
            log.debug("[CourtCache] findAllCourts -> L1_Caffeine_HIT size={}", mem.size());
            return mem;
        }
        String json = stringRedisTemplate.opsForValue().get(REDIS_LIST_ALL);
        if (json != null) {
            List<Court> list = readCourtList(json);
            if (list != null) {
                log.debug("[CourtCache] findAllCourts -> L2_Redis_HIT size={}，回写L1", list.size());
                localListAll.put(lk, list);
                return list;
            }
        }
        log.debug("[CourtCache] findAllCourts -> DB_LOAD");
        List<Court> fromDb = dbLoader.get();
        if (fromDb != null) {
            try {
                stringRedisTemplate.opsForValue().set(
                        REDIS_LIST_ALL, objectMapper.writeValueAsString(fromDb), REDIS_TTL_SECONDS, TimeUnit.SECONDS);
            } catch (JsonProcessingException e) {
                log.warn("Redis 写入全量场地列表失败", e);
            }
            localListAll.put(lk, fromDb);
        }
        return fromDb == null ? Collections.emptyList() : fromDb;
    }

    /**
     * 从 ZSet 取热度最高的 id（分数相同时按 member 字典序，Redis 规则）。
     */
    public List<Integer> getHotCourtIds(int limit) {
        if (limit <= 0) {
            return Collections.emptyList();
        }
        Set<String> raw = stringRedisTemplate.opsForZSet().reverseRange(REDIS_ZSET_HEAT, 0, (long) limit - 1);
        if (raw == null || raw.isEmpty()) {
            Set<String> fromSet = stringRedisTemplate.opsForSet().members(REDIS_SET_HOT_IDS);
            if (fromSet == null || fromSet.isEmpty()) {
                return Collections.emptyList();
            }
            return fromSet.stream()
                    .map(this::safeParseId)
                    .filter(Objects::nonNull)
                    .limit(limit)
                    .collect(Collectors.toList());
        }
        return raw.stream()
                .map(this::safeParseId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public void evictAfterCourtMutation(Integer id) {
        if (id != null) {
            localById.invalidate(id);
            stringRedisTemplate.delete(REDIS_COURT_PREFIX + id);
        }
        localListAll.invalidateAll();
        stringRedisTemplate.delete(REDIS_LIST_ALL);
    }

    private void recordHeatAsync(Integer courtId) {
        cacheTaskExecutor.execute(() -> {
            try {
                stringRedisTemplate.opsForZSet().incrementScore(REDIS_ZSET_HEAT, courtId.toString(), 1);
                Long zcard = stringRedisTemplate.opsForZSet().size(REDIS_ZSET_HEAT);
                if (zcard != null && zcard > ZSET_TRIM_THRESHOLD) {
                    long toRemove = zcard - HOT_KEEP;
                    if (toRemove > 0) {
                        // 升序：rank 0 为 score 最小，裁掉最低的 toRemove 个
                        stringRedisTemplate.opsForZSet().removeRange(REDIS_ZSET_HEAT, 0, toRemove - 1);
                    }
                    refreshHotIdSet();
                }
            } catch (Exception e) {
                log.debug("异步记录场地热度失败 courtId={}", courtId, e);
            }
        });
    }

    private void refreshHotIdSet() {
        try {
            Set<String> top = stringRedisTemplate.opsForZSet().reverseRange(REDIS_ZSET_HEAT, 0, HOT_TOP_FOR_SET - 1);
            if (top == null || top.isEmpty()) {
                return;
            }
            stringRedisTemplate.delete(REDIS_SET_HOT_IDS);
            stringRedisTemplate.opsForSet().add(REDIS_SET_HOT_IDS, top.toArray(new String[0]));
            stringRedisTemplate.expire(REDIS_SET_HOT_IDS, REDIS_TTL_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.debug("刷新热点 Set 失败", e);
        }
    }

    private void writeCourtToRedis(Integer id, Court c) {
        try {
            stringRedisTemplate.opsForValue().set(
                    REDIS_COURT_PREFIX + id, objectMapper.writeValueAsString(c), REDIS_TTL_SECONDS, TimeUnit.SECONDS);
        } catch (JsonProcessingException e) {
            log.warn("Redis 写入场地失败 id={}", id, e);
        }
    }

    private Court readCourt(String json) {
        try {
            return objectMapper.readValue(json, Court.class);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private List<Court> readCourtList(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private Integer safeParseId(String s) {
        try {
            return Integer.valueOf(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
