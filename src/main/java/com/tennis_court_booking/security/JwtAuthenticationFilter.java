// com/tennis_court_booking/security/JwtAuthenticationFilter.java
package com.tennis_court_booking.security;

import com.tennis_court_booking.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;


@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private JwtPrincipalCacheService jwtPrincipalCacheService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // 获取 token
        String token = getTokenFromRequest(request);

        if (token != null) {
            try {
                Claims claims = jwtUtil.parseClaimsIfValid(token);
                if (claims == null) {
                    logger.debug("JWT Filter - Token 无效或已过期");
                } else {
                    String username = claims.getSubject();
                    logger.debug("JWT Filter - 从 token 中提取到用户名：{}", username);

                    if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                        // ① 验签已在 parseClaimsIfValid 完成；②③ UserDetails 来自 Claims + Caffeine + Redis，不查 user 表
                        UserDetails userDetails = jwtPrincipalCacheService.resolveUserDetails(claims);

                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                        SecurityContextHolder.getContext().setAuthentication(authentication);

                        Integer userId = intClaim(claims, "userId");
                        Integer role = intClaim(claims, "role");
                        request.setAttribute("userId", userId);
                        request.setAttribute("username", username);
                        request.setAttribute("role", role);

                        logger.debug("JWT 认证成功 - userId: {}, username: {}, role: {}", userId, username, role);
                    }
                }
            } catch (Exception e) {
                logger.error("JWT 认证失败：" + e.getMessage(), e);
            }
        } else {
            logger.debug("JWT Filter - 请求中未提供 token");
        }

        filterChain.doFilter(request, response);
    }

    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private static Integer intClaim(Claims claims, String name) {
        Object v = claims.get(name);
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
}