package com.tennis_court_booking.controller;

import com.tennis_court_booking.pojo.entity.User;
import com.tennis_court_booking.pojo.vo.Result;
import com.tennis_court_booking.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 获取所有用户列表
     */
    @GetMapping("/users")
    public Result<List<User>> findAll() {
        List<User> users = userService.findAll();
        return Result.success(users);
    }

    /**
     * 新增用户
     */
    @PostMapping("/users")
    public Result<User> addUser(@RequestBody User user) {
        System.out.println("接收到新增用户请求: " + user);
        User newUser = userService.addUser(user);
        return Result.success("新增成功", newUser);
    }

    @DeleteMapping("/users/{id}")
    public Result<String> deleteById(@PathVariable Integer id) {
        userService.deleteById(id);
        return Result.success("删除成功", null);
    }

    @DeleteMapping("/users/batch")
    public Result<String> batchDelete(@RequestBody List<Integer> ids) {
        userService.batchDelete(ids);
        return Result.success("批量删除成功", null);
    }
}