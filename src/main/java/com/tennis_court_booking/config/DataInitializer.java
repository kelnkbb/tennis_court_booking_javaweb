// com/tennis_court_booking/config/DataInitializer.java
package com.tennis_court_booking.config;

import com.tennis_court_booking.mapper.UserMapper;
import com.tennis_court_booking.pojo.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // 检查是否存在管理员用户
        User admin = userMapper.findByUsername("admin");
        if (admin == null) {
            // 创建默认管理员
            User defaultAdmin = new User();
            defaultAdmin.setUsername("admin");
            // 使用 BCrypt 加密
            defaultAdmin.setPassword(passwordEncoder.encode("ch112211"));
            defaultAdmin.setPhone("00000000000");
            defaultAdmin.setEmail("admin@tennis.com");
            defaultAdmin.setRealName("系统管理员");
            defaultAdmin.setRole(2); // 管理员
            defaultAdmin.setStatus(1);
            defaultAdmin.setCreateTime(LocalDateTime.now());
            defaultAdmin.setUpdateTime(LocalDateTime.now());

            userMapper.addUser(defaultAdmin);
            System.out.println("✅ 默认管理员用户创建成功！");
            System.out.println("   用户名: admin");
            System.out.println("   密码: ch112211");
            System.out.println("   加密方式: BCrypt");
        } else {
            System.out.println("管理员用户已存在，跳过初始化");
        }
    }
}