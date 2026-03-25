// com/tennis_court_booking/controller/AuthController.java
package com.tennis_court_booking.controller;

import com.tennis_court_booking.pojo.entity.User;
import com.tennis_court_booking.pojo.vo.*;
import com.tennis_court_booking.service.UserService;
import com.tennis_court_booking.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<LoginResponseVO> login(@RequestBody LoginVO loginVO) {
        System.out.println("登录请求: " + loginVO.getUsername());

        // 1. 验证参数
        if (loginVO.getUsername() == null || loginVO.getUsername().trim().isEmpty()) {
            return Result.error(400, "用户名不能为空");
        }
        if (loginVO.getPassword() == null || loginVO.getPassword().trim().isEmpty()) {
            return Result.error(400, "密码不能为空");
        }

        // 2. 查询用户
        User user = userService.findByUsername(loginVO.getUsername());
        if (user == null) {
            return Result.error(401, "用户名或密码错误");
        }

        // 3. 使用 PasswordEncoder 验证密码
        System.out.println("输入密码: " + loginVO.getPassword());
        System.out.println("数据库密码: " + user.getPassword());

        boolean matches = passwordEncoder.matches(loginVO.getPassword(), user.getPassword());
        System.out.println("密码验证结果: " + matches);

        if (!matches) {
            return Result.error(401, "用户名或密码错误");
        }

        // 4. 检查用户状态
        if (user.getStatus() == 0) {
            return Result.error(403, "账号已被禁用");
        }

        // 5. 生成token
        String token = jwtUtil.generateToken(user.getUsername(), user.getId(), user.getRole());

        // 6. 构建返回数据（隐藏密码）
        user.setPassword(null);
        LoginResponseVO response = new LoginResponseVO();
        response.setToken(token);
        response.setUser(user);
        response.setExpiresIn(jwtUtil.getExpirationTime());

        return Result.success("登录成功", response);
    }

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public Result<User> register(@RequestBody RegisterVO registerVO) {
        System.out.println("注册请求: " + registerVO.getUsername());

        // 1. 验证参数
        if (registerVO.getUsername() == null || registerVO.getUsername().trim().isEmpty()) {
            return Result.error(400, "用户名不能为空");
        }
        if (registerVO.getPassword() == null || registerVO.getPassword().trim().isEmpty()) {
            return Result.error(400, "密码不能为空");
        }
        if (registerVO.getConfirmPassword() == null || registerVO.getConfirmPassword().trim().isEmpty()) {
            return Result.error(400, "确认密码不能为空");
        }
        if (!registerVO.getPassword().equals(registerVO.getConfirmPassword())) {
            return Result.error(400, "两次输入的密码不一致");
        }
        if (registerVO.getPhone() == null || registerVO.getPhone().trim().isEmpty()) {
            return Result.error(400, "手机号不能为空");
        }
        if (registerVO.getEmail() == null || registerVO.getEmail().trim().isEmpty()) {
            return Result.error(400, "邮箱不能为空");
        }
        if (registerVO.getRealName() == null || registerVO.getRealName().trim().isEmpty()) {
            return Result.error(400, "真实姓名不能为空");
        }

        // 2. 检查用户名是否已存在
        User existingUser = userService.findByUsername(registerVO.getUsername());
        if (existingUser != null) {
            return Result.error(409, "用户名已存在");
        }

        // 3. 检查手机号是否已存在
        existingUser = userService.findByPhone(registerVO.getPhone());
        if (existingUser != null) {
            return Result.error(409, "手机号已被注册");
        }

        // 4. 检查邮箱是否已存在
        existingUser = userService.findByEmail(registerVO.getEmail());
        if (existingUser != null) {
            return Result.error(409, "邮箱已被注册");
        }

        // 5. 创建新用户
        User user = new User();
        user.setUsername(registerVO.getUsername());

        // 使用 PasswordEncoder 加密密码
        String encodedPassword = passwordEncoder.encode(registerVO.getPassword());
        System.out.println("密码加密结果: " + encodedPassword);
        user.setPassword(encodedPassword);

        user.setPhone(registerVO.getPhone());
        user.setEmail(registerVO.getEmail());
        user.setRealName(registerVO.getRealName());
        user.setRole(1); // 默认普通用户
        user.setStatus(1); // 默认正常

        User newUser = userService.addUser(user);

        // 隐藏密码
        newUser.setPassword(null);

        return Result.success("注册成功", newUser);
    }

    /**
     * 退出登录
     */
    @PostMapping("/logout")
    public Result<String> logout(@RequestHeader(value = "Authorization", required = false) String token) {
        System.out.println("退出登录");
        return Result.success("退出成功");
    }

    /**
     * 获取当前登录用户信息
     */
    @GetMapping("/user/current")
    public Result<User> getCurrentUser(@RequestAttribute("userId") Integer userId) {
        User user = userService.findById(userId);
        if (user == null) {
            return Result.error(404, "用户不存在");
        }
        user.setPassword(null);
        return Result.success(user);
    }

    /**
     * 修改密码
     */
    @PutMapping("/user/password")
    public Result<String> changePassword(
            @RequestAttribute("userId") Integer userId,
            @RequestBody ChangePasswordVO changePasswordVO) {

        System.out.println("修改密码请求，用户ID: " + userId);

        // 1. 验证参数
        if (changePasswordVO.getOldPassword() == null || changePasswordVO.getOldPassword().trim().isEmpty()) {
            return Result.error(400, "原密码不能为空");
        }
        if (changePasswordVO.getNewPassword() == null || changePasswordVO.getNewPassword().trim().isEmpty()) {
            return Result.error(400, "新密码不能为空");
        }
        if (changePasswordVO.getNewPassword().length() < 6) {
            return Result.error(400, "新密码长度不能小于6位");
        }

        // 2. 获取用户（含密码，不走缓存）
        User user = userService.findByIdWithPassword(userId);
        if (user == null) {
            return Result.error(404, "用户不存在");
        }

        // 3. 使用 PasswordEncoder 验证原密码
        System.out.println("原密码验证");
        System.out.println("输入原密码: " + changePasswordVO.getOldPassword());
        System.out.println("数据库密码: " + user.getPassword());

        boolean matches = passwordEncoder.matches(changePasswordVO.getOldPassword(), user.getPassword());
        System.out.println("原密码验证结果: " + matches);

        if (!matches) {
            return Result.error(401, "原密码错误");
        }

        // 4. 更新密码
        String newEncoded = passwordEncoder.encode(changePasswordVO.getNewPassword());
        System.out.println("新密码加密结果: " + newEncoded);
        userService.updatePassword(userId, newEncoded);

        return Result.success("密码修改成功");
    }
}