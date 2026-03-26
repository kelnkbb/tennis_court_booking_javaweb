package com.tennis_court_booking.pojo.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 按日订单统计
 */
@Data
public class OrderDailyStatsVO {

    private LocalDate statDate;
    private Integer orderCount;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
}
