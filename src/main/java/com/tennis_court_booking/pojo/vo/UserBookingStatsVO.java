package com.tennis_court_booking.pojo.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class UserBookingStatsVO {
    private Integer userId;
    private String username;
    private String realName;
    private String phone;

    /** 预约数（按筛选范围） */
    private Integer bookingCount;
    /** 总时长（小时，可能为小数） */
    private BigDecimal totalHours;
    /** 总金额 */
    private BigDecimal totalAmount;

    /** 已付款单数 */
    private Integer paidCount;
    /** 已付款金额 */
    private BigDecimal paidAmount;

    /** 已取消单数 */
    private Integer canceledCount;
}

