package com.tennis_court_booking.service.impl;

import com.tennis_court_booking.cache.UserCacheManager;
import com.tennis_court_booking.mapper.UserMapper;
import com.tennis_court_booking.pojo.entity.User;
import com.tennis_court_booking.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserCacheManager userCacheManager;

    @Override
    public List<User> findAll() {
        List<User> userlist = userMapper.findAll();
        return userlist;
    }

    @Override
    public User addUser(User user) {
        System.out.println("Service层新增用户: " + user);

        // 设置创建时间和更新时间
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());

        // 设置默认值
        if (user.getStatus() == null) {
            user.setStatus(1); // 默认正常
        }
        if (user.getRole() == null) {
            user.setRole(1); // 默认普通用户
        }

        // 插入数据库
        userMapper.addUser(user);
        System.out.println("新增成功，生成ID: " + user.getId());

        return user;
    }

    // 在 UserServiceImpl.java 中添加
    @Override
    public User findByUsername(String username) {
        return userMapper.findByUsername(username);
    }

    @Override
    public User findByPhone(String phone) {
        User u = userMapper.findByPhone(phone);
        if (u == null) {
            return null;
        }
        return userCacheManager.getUserById(u.getId(), () -> userMapper.findById(u.getId()));
    }

    @Override
    public User findByEmail(String email) {
        User u = userMapper.findByEmail(email);
        if (u == null) {
            return null;
        }
        return userCacheManager.getUserById(u.getId(), () -> userMapper.findById(u.getId()));
    }

    @Override
    public User findById(Integer id) {
        return userCacheManager.getUserById(id, () -> userMapper.findById(id));
    }

    @Override
    public User findByIdWithPassword(Integer id) {
        if (id == null) {
            return null;
        }
        return userMapper.findById(id);
    }

    @Override
    public void updatePassword(Integer userId, String newPassword) {
        userMapper.updatePassword(userId, newPassword);
        userCacheManager.evictAfterUserMutation(userId);
    }

}