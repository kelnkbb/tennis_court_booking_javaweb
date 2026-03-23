package com.tennis_court_booking.limit.service;

/**
 * Redis 滑动窗口限流接口。
 */
public interface SlidingWindowRateLimiter {

    /**
     * @return true=允许；false=限流
     */
    boolean tryAcquire(String limitKey, int windowSeconds, int maxRequests);
}

