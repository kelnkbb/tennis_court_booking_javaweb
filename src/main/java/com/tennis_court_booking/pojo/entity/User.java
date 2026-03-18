package com.tennis_court_booking.pojo.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class User {
    private Integer id;
    private String username;
    private String password;
    private String phone;
    private String email;
    private String realName;      // 对应数据库 real_name 字段
    private Integer role;          // 1:普通用户 2:管理员
    private Integer status;        // 1:正常 0:禁用
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}