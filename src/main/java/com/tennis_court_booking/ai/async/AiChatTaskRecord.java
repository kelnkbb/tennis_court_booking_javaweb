package com.tennis_court_booking.ai.async;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Redis 中保存的任务状态（供轮询查询）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiChatTaskRecord {

    public static final String ST_PENDING = "PENDING";
    public static final String ST_PROCESSING = "PROCESSING";
    public static final String ST_SUCCEEDED = "SUCCEEDED";
    public static final String ST_FAILED = "FAILED";
    /** 已进入死信队列，需人工/补偿处理 */
    public static final String ST_DEAD = "DEAD";

    private String taskId;
    private Integer userId;
    private String status;
    /** 模型原始返回（含 ---QUICK--- 等） */
    private String answerRaw;
    private String errorMessage;
    /** 与 MQ 载荷一致，供 DEAD/FAILED 补偿重投 */
    private String memoryId;
    private String userMessage;
    /** 补偿重投次数（含管理员与用户触发） */
    private int compensateAttempts;
    private long createdAtEpochMs;
    private long updatedAtEpochMs;
}
