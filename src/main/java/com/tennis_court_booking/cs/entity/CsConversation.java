package com.tennis_court_booking.cs.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CsConversation {
    private Long id;
    private Integer userId;
    /** 1=进行中 0=已结束 */
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
