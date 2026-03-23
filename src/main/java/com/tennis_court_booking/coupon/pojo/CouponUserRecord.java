package com.tennis_court_booking.coupon.pojo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CouponUserRecord {
    private Long id;
    private Integer activityId;
    private Integer userId;
    private String couponCode;
    private LocalDateTime grabTime;
    private Integer useStatus;
    /** 使用该券的预约单 ID */
    private Integer bookingId;
}
