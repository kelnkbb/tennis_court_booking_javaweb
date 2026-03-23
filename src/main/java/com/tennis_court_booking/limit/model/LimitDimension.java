package com.tennis_court_booking.limit.model;

/**
 * 限流维度：
 * GLOBAL 全局，IP 按来源 IP，USER 按登录用户。
 */
public enum LimitDimension {
    GLOBAL,
    IP,
    USER
}
