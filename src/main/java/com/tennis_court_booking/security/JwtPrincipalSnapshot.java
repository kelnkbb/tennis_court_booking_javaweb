package com.tennis_court_booking.security;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 仅存 JWT 中已有字段，用于 Redis 序列化；不替代数据库用户表。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JwtPrincipalSnapshot {
    private String username;
    private Integer userId;
    private Integer role;
}
