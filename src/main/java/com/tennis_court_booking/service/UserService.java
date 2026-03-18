package com.tennis_court_booking.service;

import com.tennis_court_booking.pojo.entity.User;
import java.util.List;

public interface UserService {
    /**
     * 查询所有用户
     */
    List<User> findAll();

    /**
     * 新增用户
     */
    User addUser(User user);

    User findByUsername(String username);
    User findByPhone(String phone);
    User findByEmail(String email);
    User findById(Integer id);
    void updatePassword(Integer userId, String newPassword);

}