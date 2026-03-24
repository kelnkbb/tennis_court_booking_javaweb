package com.tennis_court_booking.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;
import java.util.List;

/**
 * 前端跨域与 WebSocket 握手允许的 Origin（逗号分隔）。
 * 部署到 Vercel 时通过环境变量 {@code APP_CORS_ALLOWED_ORIGINS} 追加 https://你的项目.vercel.app
 */
@ConfigurationProperties(prefix = "app.cors")
public class CorsProperties {

    /**
     * 逗号分隔，例如：http://localhost:5173,https://xxx.vercel.app
     */
    private String allowedOrigins = "http://localhost:5173,http://127.0.0.1:5173";

    public String getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(String allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    public List<String> originList() {
        return Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    public String[] originArray() {
        List<String> list = originList();
        return list.toArray(new String[0]);
    }
}
