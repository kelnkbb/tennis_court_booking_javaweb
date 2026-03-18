package com.tennis_court_booking.mapper;

import com.tennis_court_booking.pojo.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserMapper {

    List<User> findAll();

    void addUser(User user);

    // 在 UserMapper.java 中添加
    User findByUsername(@Param("username") String username);
    User findByPhone(@Param("phone") String phone);
    User findByEmail(@Param("email") String email);
    User findById(@Param("id") Integer id);
    void updatePassword(@Param("userId") Integer userId, @Param("newPassword") String newPassword);

}