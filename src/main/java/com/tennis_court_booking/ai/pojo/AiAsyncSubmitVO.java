package com.tennis_court_booking.ai.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiAsyncSubmitVO {
    private String taskId;
    /** 提示前端轮询的相对路径 */
    private String statusPath;
}
