package com.tennis_court_booking.ai.async;

import lombok.Data;

/**
 * 管理端查看异步任务列表（补偿排查）。
 */
@Data
public class AiChatTaskAdminSummaryVO {
    private String taskId;
    private Integer userId;
    private String status;
    private String errorMessage;
    private String memoryId;
    private String userMessage;
    private int compensateAttempts;
    private long createdAtEpochMs;
    private long updatedAtEpochMs;
}
