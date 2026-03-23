package com.tennis_court_booking.coupon.pojo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CouponActivity {
    private Integer id;
    private String title;
    private Integer totalStock;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal discountAmount;
    /** 0草稿 1已发布 2已结束 */
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
