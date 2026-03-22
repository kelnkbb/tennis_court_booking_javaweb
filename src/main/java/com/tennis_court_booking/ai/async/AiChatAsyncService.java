package com.tennis_court_booking.ai.async;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.tennis_court_booking.ai.util.AiReplyParser;
import com.tennis_court_booking.pojo.vo.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatAsyncService {

    private final RabbitTemplate rabbitTemplate;
    private final AiChatTaskStore taskStore;

    /**
     * 投递异步任务，立即返回 taskId；客户端轮询 {@link #getTaskForUser(String, Integer)}。
     */
    public String submit(Integer userId, String memoryId, String message) throws JsonProcessingException {
        String taskId = UUID.randomUUID().toString().replace("-", "");
        long now = System.currentTimeMillis();
        AiChatTaskRecord record = new AiChatTaskRecord(
                taskId, userId, AiChatTaskRecord.ST_PENDING, null, null,
                memoryId, message, 0, now, now);
        taskStore.save(record);

        sendWorkMessage(new AiChatTaskMessage(taskId, userId, memoryId, message, now));
        return taskId;
    }

    public Result<AiChatTaskStatusVO> getTaskForUser(String taskId, Integer userId) {
        return taskStore.find(taskId)
                .filter(r -> r.getUserId() != null && r.getUserId().equals(userId))
                .map(this::toVo)
                .map(Result::success)
                .orElse(Result.error(404, "任务不存在或无权查看"));
    }

    private AiChatTaskStatusVO toVo(AiChatTaskRecord r) {
        AiChatTaskStatusVO vo = new AiChatTaskStatusVO();
        vo.setTaskId(r.getTaskId());
        vo.setStatus(r.getStatus());
        vo.setErrorMessage(r.getErrorMessage());
        vo.setCreatedAtEpochMs(r.getCreatedAtEpochMs());
        vo.setUpdatedAtEpochMs(r.getUpdatedAtEpochMs());
        if (AiChatTaskRecord.ST_SUCCEEDED.equals(r.getStatus()) && r.getAnswerRaw() != null) {
            vo.setReply(AiReplyParser.parse(r.getAnswerRaw()));
        }
        return vo;
    }

    /**
     * 投递工作队列（补偿重投与 {@link #submit} 共用）。
     */
    public void sendWorkMessage(AiChatTaskMessage payload) {
        try {
            rabbitTemplate.convertAndSend(AiRabbitMqConfig.EXCHANGE, AiRabbitMqConfig.ROUTING_WORK, payload);
            log.info("AI 异步任务已投递 taskId={} userId={}", payload.getTaskId(), payload.getUserId());
        } catch (AmqpException e) {
            log.error("AI 异步任务投递 MQ 失败 taskId={} userId={}", payload.getTaskId(), payload.getUserId(), e);
            try {
                taskStore.mergeUpdate(payload.getTaskId(), r -> {
                    r.setStatus(AiChatTaskRecord.ST_FAILED);
                    r.setErrorMessage("消息队列暂不可用，请稍后重试");
                });
            } catch (JsonProcessingException ex) {
                log.error("MQ 失败后更新任务状态写入 Redis 异常 taskId={}", payload.getTaskId(), ex);
            }
        }
    }

}
