package com.tennis_court_booking.cs.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CsConversationVO {
    private Long id;
    private Integer userId;
    /** 会话用户侧账号名（管理员列表展示） */
    private String username;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
