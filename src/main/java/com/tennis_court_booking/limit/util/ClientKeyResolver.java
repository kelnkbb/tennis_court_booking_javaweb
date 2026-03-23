package com.tennis_court_booking.limit.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 生成限流维度的 value（如 IP/userId）。
 */
public interface ClientKeyResolver {

    String resolveIp(HttpServletRequest request);

    String resolveUserId(HttpServletRequest request);
}

