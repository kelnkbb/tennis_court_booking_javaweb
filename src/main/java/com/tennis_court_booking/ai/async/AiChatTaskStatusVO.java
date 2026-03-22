package com.tennis_court_booking.ai.async;

import com.tennis_court_booking.ai.pojo.AiChatReply;
import lombok.Data;

@Data
public class AiChatTaskStatusVO {
    private String taskId;
    private String status;
    private AiChatReply reply;
    private String errorMessage;
    private long createdAtEpochMs;
    private long updatedAtEpochMs;
}
