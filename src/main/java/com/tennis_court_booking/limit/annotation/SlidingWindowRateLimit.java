package com.tennis_court_booking.limit.annotation;

import com.tennis_court_booking.limit.model.LimitDimension;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 滑动窗口限流注解（Redis 计数）。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SlidingWindowRateLimit {

    /**
     * 限流业务键（建议唯一，如 coupon:grab）。
     */
    String key();

    /** 时间窗口（秒） */
    int windowSeconds();

    /** 窗口内最大请求数 */
    int maxRequests();

    /**
     * 限流维度（全局/IP/用户），用于拼接最终限流 key。
     */
    LimitDimension[] dimensions() default {LimitDimension.GLOBAL};

    /** 触发限流时返回消息 */
    String message() default "请求过于频繁，请稍后再试";
}
