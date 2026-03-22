package com.tennis_court_booking.ai.async;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 投递到 RabbitMQ 的异步对话任务载荷。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiChatTaskMessage implements Serializable {

    private String taskId;
    private Integer userId;
    /** 与同步接口一致：u:{userId} */
    private String memoryId;
    private String message;
    private long createdAtEpochMs;
}
