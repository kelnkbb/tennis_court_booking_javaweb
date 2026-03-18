package com.tennis_court_booking.pojo.vo;

import lombok.Data;

@Data
public class Result<T> {
    // 常用 HTTP 状态码常量
    public static final int SUCCESS = 200;
    public static final int UNAUTHORIZED = 401;
    public static final int FORBIDDEN = 403;
    public static final int NOT_FOUND = 404;
    public static final int ERROR = 500;

    private Integer code;      // 状态码
    private String message;    // 提示信息
    private T data;            // 返回数据（泛型）

    // 私有构造，强制使用静态方法
    private Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    // 成功，无返回数据
    public static <T> Result<T> success() {
        return new Result<>(200, "操作成功", null);
    }

    // 成功，有返回数据
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "操作成功", data);
    }

    // 成功，自定义消息
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(200, message, data);
    }

    // 失败
    public static <T> Result<T> error(String message) {
        return new Result<>(500, message, null);
    }

    // 失败，自定义状态码
    public static <T> Result<T> error(Integer code, String message) {
        return new Result<>(code, message, null);
    }
}
