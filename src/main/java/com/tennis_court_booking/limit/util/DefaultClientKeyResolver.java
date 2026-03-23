package com.tennis_court_booking.limit.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

/**
 * 默认维度解析：
 * - IP：取 X-Forwarded-For / remoteAddr
 * - USER：从 JWT filter 已放入 request attribute 的 userId
 */
@Component
public class DefaultClientKeyResolver implements ClientKeyResolver {

    @Override
    public String resolveIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) {
            return xf.split(",")[0].trim();
        }
        String ip = request.getRemoteAddr();
        return ip == null ? "" : ip;
    }

    @Override
    public String resolveUserId(HttpServletRequest request) {
        Object v = request.getAttribute("userId");
        if (v == null) {
            return "";
        }
        return String.valueOf(v);
    }
}

