package com.tennis_court_booking.coupon.async;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 抢购结果（写入 Redis，供客户端轮询）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeckillGrabRedisResult {

    public static final String ST_PENDING = "PENDING";
    public static final String ST_SUCCESS = "SUCCESS";
    public static final String ST_FAILED = "FAILED";

    private String status;
    private String message;
    private String couponCode;
    private Integer activityId;
    private Integer userId;

    public static SeckillGrabRedisResult pending(Integer activityId, Integer userId) {
        return new SeckillGrabRedisResult(ST_PENDING, null, null, activityId, userId);
    }

    public static SeckillGrabRedisResult success(String message, String couponCode, Integer activityId, Integer userId) {
        return new SeckillGrabRedisResult(ST_SUCCESS, message, couponCode, activityId, userId);
    }

    public static SeckillGrabRedisResult failed(String message, Integer activityId, Integer userId) {
        return new SeckillGrabRedisResult(ST_FAILED, message, null, activityId, userId);
    }
}
