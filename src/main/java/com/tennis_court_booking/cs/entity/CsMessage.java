package com.tennis_court_booking.cs.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CsMessage {
    private Long id;
    private Long conversationId;
    /** 1=用户 2=客服 */
    private Integer senderType;
    private Integer senderUserId;
    private String content;
    private LocalDateTime createdAt;
}
