package com.tennis_court_booking.limit.exception;

import lombok.Getter;

/**
 * 限流异常。
 */
@Getter
public class RateLimitException extends RuntimeException {

    private final int statusCode;

    public RateLimitException(String message) {
        this(message, 429);
    }

    public RateLimitException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }
}

