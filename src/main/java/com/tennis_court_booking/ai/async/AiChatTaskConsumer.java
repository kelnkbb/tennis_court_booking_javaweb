package com.tennis_court_booking.ai.async;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.tennis_court_booking.ai.context.AiRequestContext;
import com.tennis_court_booking.ai.service.SimpleAiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 工作队列消费者：调用 LangChain4j；失败抛异常由 Spring Retry + DLX 处理。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiChatTaskConsumer {

    private final SimpleAiService simpleAiService;
    private final AiChatTaskStore taskStore;

    @RabbitListener(queues = AiRabbitMqConfig.QUEUE_WORK)
    public void handle(AiChatTaskMessage msg) throws JsonProcessingException {
        log.info("AI 异步任务开始处理 taskId={} userId={}", msg.getTaskId(), msg.getUserId());

        taskStore.mergeUpdate(msg.getTaskId(), r -> {
            r.setStatus(AiChatTaskRecord.ST_PROCESSING);
            r.setErrorMessage(null);
        });

        try {
            AiRequestContext.setUserId(msg.getUserId());
            String answer = simpleAiService.chat(msg.getMemoryId(), msg.getMessage());
            taskStore.mergeUpdate(msg.getTaskId(), r -> {
                r.setStatus(AiChatTaskRecord.ST_SUCCEEDED);
                r.setAnswerRaw(answer);
                r.setErrorMessage(null);
            });
            log.info("AI 异步任务完成 taskId={}", msg.getTaskId());
        } catch (Exception e) {
            log.warn("AI 异步任务执行异常 taskId={} msg={}", msg.getTaskId(), e.getMessage());
            try {
                taskStore.mergeUpdate(msg.getTaskId(), r -> {
                    r.setStatus(AiChatTaskRecord.ST_FAILED);
                    r.setErrorMessage(e.getClass().getSimpleName() + ": " + e.getMessage());
                });
            } catch (JsonProcessingException ex) {
                log.error("更新任务失败状态写入 Redis 异常", ex);
            }
            throw new RuntimeException(e);
        } finally {
            AiRequestContext.clear();
        }
    }
}
