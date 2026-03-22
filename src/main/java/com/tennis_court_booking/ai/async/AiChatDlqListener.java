package com.tennis_court_booking.ai.async;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 死信队列：重试耗尽后消息到达此处，更新 Redis 状态为 DEAD，便于人工排查或后续补偿任务扫描。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiChatDlqListener {

    private final AiChatTaskStore taskStore;

    @RabbitListener(queues = AiRabbitMqConfig.QUEUE_DLQ)
    public void onDead(AiChatTaskMessage msg) {
        log.error("AI 异步任务进入死信队列 taskId={} userId={}", msg.getTaskId(), msg.getUserId());
        try {
            taskStore.find(msg.getTaskId()).ifPresentOrElse(r -> {
                r.setStatus(AiChatTaskRecord.ST_DEAD);
                r.setErrorMessage("多次重试失败，已进入死信队列，请联系管理员或稍后重试");
                r.setUpdatedAtEpochMs(System.currentTimeMillis());
                try {
                    taskStore.save(r);
                } catch (JsonProcessingException e) {
                    log.error("DLQ 写 Redis 失败 taskId={}", msg.getTaskId(), e);
                }
            }, () -> log.warn("DLQ 收到消息但 Redis 无任务记录 taskId={}", msg.getTaskId()));
        } catch (Exception e) {
            log.error("DLQ 处理异常 taskId={}", msg.getTaskId(), e);
        }
    }
}
