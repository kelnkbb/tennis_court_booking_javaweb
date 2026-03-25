package com.tennis_court_booking.cs.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CsMessageVO {
    private Long id;
    private Long conversationId;
    private Integer senderType;
    private Integer senderUserId;
    private String content;
    private LocalDateTime createdAt;
}
