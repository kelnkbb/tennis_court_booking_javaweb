package com.tennis_court_booking.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 单独提供 PasswordEncoder，避免与 {@link SecurityConfig} 形成循环依赖：
 * SecurityConfig → CustomUserDetailsService → UserServiceImpl → PasswordEncoder（原在 SecurityConfig 内）。
 */
@Configuration
public class PasswordEncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
