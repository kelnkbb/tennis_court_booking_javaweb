package com.tennis_court_booking.coupon.pojo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CouponUnusedVO {
    private Long id;
    private String couponCode;
    private String activityTitle;
    private BigDecimal discountAmount;
}
