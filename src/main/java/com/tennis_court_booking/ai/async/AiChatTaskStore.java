package com.tennis_court_booking.ai.async;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class AiChatTaskStore {

    private static final String KEY_PREFIX = "ai:chat:task:";
    private static final long TTL_HOURS = 24;

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public void save(AiChatTaskRecord record) throws JsonProcessingException {
        String key = KEY_PREFIX + record.getTaskId();
        stringRedisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(record), TTL_HOURS, TimeUnit.HOURS);
    }

    public Optional<AiChatTaskRecord> find(String taskId) {
        try {
            String json = stringRedisTemplate.opsForValue().get(KEY_PREFIX + taskId);
            if (json == null) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(json, AiChatTaskRecord.class));
        } catch (JsonProcessingException e) {
            return Optional.empty();
        }
    }

    public void mergeUpdate(String taskId, java.util.function.Consumer<AiChatTaskRecord> mutator) throws JsonProcessingException {
        AiChatTaskRecord r = find(taskId).orElseThrow(() -> new IllegalStateException("task not found: " + taskId));
        mutator.accept(r);
        r.setUpdatedAtEpochMs(System.currentTimeMillis());
        save(r);
    }

    /**
     * 扫描任务 key，按状态筛选（管理端补偿列表；数据量大时慎用 limit）。
     */
    public List<AiChatTaskRecord> findRecordsByStatuses(int maxResults, String... statuses) {
        if (maxResults <= 0) {
            maxResults = 200;
        }
        Set<String> st = new HashSet<>(Arrays.asList(statuses));
        List<AiChatTaskRecord> out = new ArrayList<>();
        ScanOptions options = ScanOptions.scanOptions().match(KEY_PREFIX + "*").count(200).build();
        try (Cursor<String> cursor = stringRedisTemplate.scan(options)) {
            while (cursor.hasNext() && out.size() < maxResults) {
                String key = cursor.next();
                String taskId = key.substring(KEY_PREFIX.length());
                find(taskId).filter(r -> st.contains(r.getStatus())).ifPresent(out::add);
            }
        }
        return out;
    }
}
