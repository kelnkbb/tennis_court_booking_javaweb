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
    /**
     * 多级缓存（L1+L2），不含密码字段。
     */
    User findById(Integer id);

    /**
     * 直查数据库，含密码哈希；仅用于原密码校验等，勿用于对外展示。
     */
    User findByIdWithPassword(Integer id);

    void updatePassword(Integer userId, String newPassword);

    void deleteById(Integer id);

    void batchDelete(List<Integer> ids);

}