package com.tennis_court_booking.limit.exception;

import com.tennis_court_booking.pojo.vo.Result;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常映射：将限流异常转换为 Result（HTTP 429）。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RateLimitException.class)
    public Result<?> handleRateLimit(RateLimitException e) {
        return Result.error(e.getStatusCode(), e.getMessage());
    }
}

