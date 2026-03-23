package com.tennis_court_booking.cache.template;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 统一模板：逻辑过期 + 异步重建（防击穿） + 空值缓存（防穿透）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisLogicalExpireTemplate {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public void evictKey(String key) {
        stringRedisTemplate.delete(Objects.requireNonNull(key));
    }

    public record CacheOptions(
            long logicalTtlSeconds,
            long physicalTtlSeconds,
            long emptyLogicalTtlSeconds,
            long rebuildLockSeconds
    ) {}

    public <T> T queryObject(
            String key,
            String rebuildLockPrefix,
            Class<T> type,
            Supplier<T> dbLoader,
            Executor rebuildExecutor,
            CacheOptions options) {
        return queryInternal(
                key, rebuildLockPrefix, dbLoader, rebuildExecutor, options,
                node -> objectMapper.treeToValue(node, type));
    }

    public <T> T queryWithTypeRef(
            String key,
            String rebuildLockPrefix,
            TypeReference<T> typeRef,
            Supplier<T> dbLoader,
            Executor rebuildExecutor,
            CacheOptions options) {
        return queryInternal(
                key, rebuildLockPrefix, dbLoader, rebuildExecutor, options,
                node -> objectMapper.readValue(node.traverse(), typeRef));
    }

    private interface NodeReader<T> {
        T read(JsonNode node) throws Exception;
    }

    private <T> T queryInternal(
            String key,
            String rebuildLockPrefix,
            Supplier<T> dbLoader,
            Executor rebuildExecutor,
            CacheOptions options,
            NodeReader<T> reader) {
        String json = stringRedisTemplate.opsForValue().get(Objects.requireNonNull(key));
        if (json != null) {
            CacheEnvelope<T> envelope = readEnvelope(json, reader);
            if (envelope != null) {
                if (envelope.empty && !envelope.isLogicalExpired()) {
                    return null;
                }
                if (envelope.data != null) {
                    if (envelope.isLogicalExpired()) {
                        tryAsyncRebuild(key, rebuildLockPrefix, dbLoader, rebuildExecutor, options);
                    }
                    return envelope.data;
                }
            }
        }

        T fromDb = dbLoader.get();
        if (fromDb == null) {
            writeEmpty(key, options);
            return null;
        }
        writeNonEmpty(key, fromDb, options);
        return fromDb;
    }

    private <T> void tryAsyncRebuild(
            String key,
            String rebuildLockPrefix,
            Supplier<T> dbLoader,
            Executor rebuildExecutor,
            CacheOptions options) {
        String lockKey = rebuildLockPrefix + key;
        Boolean locked = stringRedisTemplate.opsForValue().setIfAbsent(
                Objects.requireNonNull(lockKey), "1", options.rebuildLockSeconds(), TimeUnit.SECONDS);
        if (!Boolean.TRUE.equals(locked)) {
            return;
        }
        rebuildExecutor.execute(() -> {
            try {
                T fresh = dbLoader.get();
                if (fresh == null) {
                    writeEmpty(key, options);
                } else {
                    writeNonEmpty(key, fresh, options);
                }
            } catch (Exception e) {
                log.warn("后台重建缓存失败 key={}", key, e);
            } finally {
                stringRedisTemplate.delete(lockKey);
            }
        });
    }

    private <T> void writeNonEmpty(String key, T data, CacheOptions options) {
        try {
            String json = objectMapper.writeValueAsString(CacheEnvelope.nonEmpty(data, options.logicalTtlSeconds()));
            stringRedisTemplate.opsForValue().set(
                    Objects.requireNonNull(key),
                    Objects.requireNonNull(json),
                    options.physicalTtlSeconds(),
                    TimeUnit.SECONDS);
        } catch (JsonProcessingException e) {
            log.warn("写入缓存失败 key={}", key, e);
        }
    }

    private void writeEmpty(String key, CacheOptions options) {
        try {
            String json = objectMapper.writeValueAsString(CacheEnvelope.empty(options.emptyLogicalTtlSeconds()));
            stringRedisTemplate.opsForValue().set(
                    Objects.requireNonNull(key),
                    Objects.requireNonNull(json),
                    options.emptyLogicalTtlSeconds(),
                    TimeUnit.SECONDS);
        } catch (JsonProcessingException e) {
            log.warn("写入空值缓存失败 key={}", key, e);
        }
    }

    private <T> CacheEnvelope<T> readEnvelope(String json, NodeReader<T> reader) {
        try {
            JsonNode root = objectMapper.readTree(json);
            if (!root.has("expireAtEpochSeconds")) {
                return null;
            }
            Long expireAt = root.path("expireAtEpochSeconds").asLong();
            boolean empty = root.path("empty").asBoolean(false);
            T data = null;
            JsonNode dataNode = root.get("data");
            if (dataNode != null && !dataNode.isNull()) {
                data = reader.read(dataNode);
            }
            return new CacheEnvelope<>(expireAt, empty, data);
        } catch (Exception e) {
            return null;
        }
    }

    private static class CacheEnvelope<T> {
        public Long expireAtEpochSeconds;
        public boolean empty;
        public T data;

        CacheEnvelope(Long expireAtEpochSeconds, boolean empty, T data) {
            this.expireAtEpochSeconds = expireAtEpochSeconds;
            this.empty = empty;
            this.data = data;
        }

        static <T> CacheEnvelope<T> nonEmpty(T data, long logicalTtlSeconds) {
            return new CacheEnvelope<>(Instant.now().getEpochSecond() + logicalTtlSeconds, false, data);
        }

        static <T> CacheEnvelope<T> empty(long logicalTtlSeconds) {
            return new CacheEnvelope<>(Instant.now().getEpochSecond() + logicalTtlSeconds, true, null);
        }

        boolean isLogicalExpired() {
            return expireAtEpochSeconds == null || expireAtEpochSeconds <= Instant.now().getEpochSecond();
        }
    }
}
