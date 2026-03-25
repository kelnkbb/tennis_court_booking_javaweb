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
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
