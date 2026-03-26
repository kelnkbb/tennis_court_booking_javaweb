package com.tennis_court_booking.pojo.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单汇总统计（按预约日期 booking_date 筛选）
 */
@Data
public class OrderStatsSummaryVO {

    /** 订单总数（预约单条数） */
    private Integer totalOrders;

    private Integer canceledCount;
    /** 待付款 */
    private Integer pendingPaymentCount;
    /** 已付款 */
    private Integer paidCount;
    /** 已完成 */
    private Integer completedCount;
    /** 已过期 */
    private Integer expiredCount;

    /** 订单金额合计（各状态均计入票面 total_amount） */
    private BigDecimal totalAmount;
    /** 已收款金额（状态为已付款、已完成） */
    private BigDecimal paidAmount;
    /** 预约总时长（小时） */
    private BigDecimal totalHours;
}
