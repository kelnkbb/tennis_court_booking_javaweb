package com.tennis_court_booking.ai.async;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.tennis_court_booking.pojo.vo.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * DEAD / FAILED 异步任务的补偿重投（重新置为 PENDING 并投递工作队列）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatTaskCompensationService {

    private static final String[] RETRYABLE = {AiChatTaskRecord.ST_DEAD, AiChatTaskRecord.ST_FAILED};

    private final AiChatTaskStore taskStore;
    private final AiChatAsyncService aiChatAsyncService;

    @Value("${ai.chat.compensation.max-attempts:5}")
    private int maxCompensateAttempts;

    /**
     * 用户：仅可重试本人任务。
     */
    public Result<Void> retryForUser(String taskId, Integer userId) {
        return retry(taskId, userId, false);
    }

    /**
     * 管理员：可重试任意任务。
     */
    public Result<Void> retryForAdmin(String taskId) {
        return retry(taskId, null, true);
    }

    private Result<Void> retry(String taskId, Integer requesterUserId, boolean admin) {
        Optional<AiChatTaskRecord> opt = taskStore.find(taskId);
        if (opt.isEmpty()) {
            return Result.error(404, "任务不存在");
        }
        AiChatTaskRecord r = opt.get();
        if (!admin) {
            if (requesterUserId == null || !Objects.equals(r.getUserId(), requesterUserId)) {
                return Result.error(403, "无权操作该任务");
            }
        }
        String st = r.getStatus();
        if (!AiChatTaskRecord.ST_DEAD.equals(st) && !AiChatTaskRecord.ST_FAILED.equals(st)) {
            return Result.error(400, "仅支持对失败或死信状态的任务重试，当前状态：" + st);
        }
        if (!StringUtils.hasText(r.getMemoryId()) || !StringUtils.hasText(r.getUserMessage())) {
            return Result.error(400, "任务缺少补偿所需字段（历史任务无 memoryId/userMessage），无法重投");
        }
        if (r.getCompensateAttempts() >= maxCompensateAttempts) {
            return Result.error(429, "已达最大补偿次数（" + maxCompensateAttempts + "），请联系管理员");
        }

        try {
            taskStore.mergeUpdate(taskId, t -> {
                t.setStatus(AiChatTaskRecord.ST_PENDING);
                t.setAnswerRaw(null);
                t.setErrorMessage(null);
                t.setCompensateAttempts(t.getCompensateAttempts() + 1);
            });
        } catch (JsonProcessingException e) {
            log.error("补偿更新 Redis 失败 taskId={}", taskId, e);
            return Result.error(500, "任务状态更新失败");
        }

        AiChatTaskMessage payload = new AiChatTaskMessage(
                r.getTaskId(),
                r.getUserId(),
                r.getMemoryId(),
                r.getUserMessage(),
                r.getCreatedAtEpochMs());
        aiChatAsyncService.sendWorkMessage(payload);
        log.info("AI 异步任务已补偿重投 taskId={} userId={} attemptNo={}", taskId, r.getUserId(), r.getCompensateAttempts() + 1);
        return Result.success();
    }

    public List<AiChatTaskAdminSummaryVO> listForAdmin(String statusFilter, int limit) {
        List<AiChatTaskRecord> rows;
        if (!StringUtils.hasText(statusFilter) || "ALL".equalsIgnoreCase(statusFilter.trim())) {
            rows = taskStore.findRecordsByStatuses(limit, RETRYABLE);
        } else {
            rows = taskStore.findRecordsByStatuses(limit, statusFilter.trim().toUpperCase());
        }
        return rows.stream().map(this::toAdminVo).collect(Collectors.toList());
    }

    private AiChatTaskAdminSummaryVO toAdminVo(AiChatTaskRecord r) {
        AiChatTaskAdminSummaryVO vo = new AiChatTaskAdminSummaryVO();
        vo.setTaskId(r.getTaskId());
        vo.setUserId(r.getUserId());
        vo.setStatus(r.getStatus());
        vo.setErrorMessage(r.getErrorMessage());
        vo.setMemoryId(r.getMemoryId());
        vo.setUserMessage(r.getUserMessage());
        vo.setCompensateAttempts(r.getCompensateAttempts());
        vo.setCreatedAtEpochMs(r.getCreatedAtEpochMs());
        vo.setUpdatedAtEpochMs(r.getUpdatedAtEpochMs());
        return vo;
    }
}
