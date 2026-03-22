package com.tennis_court_booking.security;

import io.jsonwebtoken.Claims;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.List;

/**
 * 仅用 JWT 载荷构造 {@link UserDetails}，不访问数据库。
 * 账号禁用/改角色需等 Token 过期或另行做黑名单（当前未实现）。
 */
public final class JwtUserDetailsFactory {

    private JwtUserDetailsFactory() {
    }

    public static UserDetails fromClaims(Claims claims) {
        String username = claims.getSubject();
        Integer role = toInt(claims.get("role"));
        return build(username, role);
    }

    private static Integer toInt(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof Integer) {
            return (Integer) v;
        }
        if (v instanceof Long) {
            return ((Long) v).intValue();
        }
        if (v instanceof Number) {
            return ((Number) v).intValue();
        }
        return null;
    }

    public static UserDetails fromSnapshot(JwtPrincipalSnapshot snap) {
        if (snap == null || snap.getUsername() == null) {
            return null;
        }
        return build(snap.getUsername(), snap.getRole());
    }

    private static UserDetails build(String username, Integer role) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        if (role != null && role == 2) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        } else {
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        }
        return User.builder()
                .username(username)
                .password("{noop}")
                .authorities(authorities)
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(false)
                .build();
    }
}
