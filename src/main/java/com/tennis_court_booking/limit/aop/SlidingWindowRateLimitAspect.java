package com.tennis_court_booking.limit.aop;

import com.tennis_court_booking.limit.annotation.SlidingWindowRateLimit;
import com.tennis_court_booking.limit.exception.RateLimitException;
import com.tennis_court_booking.limit.model.LimitDimension;
import com.tennis_court_booking.limit.service.SlidingWindowRateLimiter;
import com.tennis_court_booking.limit.util.ClientKeyResolver;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.Collectors;

/**
 * Redis 滑动窗口限流 AOP。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class SlidingWindowRateLimitAspect {

    private final SlidingWindowRateLimiter slidingWindowRateLimiter;
    private final ClientKeyResolver clientKeyResolver;

    @Around("@annotation(com.tennis_court_booking.limit.annotation.SlidingWindowRateLimit)")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        MethodSignature ms = (MethodSignature) pjp.getSignature();
        Method method = ms.getMethod();
        SlidingWindowRateLimit ann = method.getAnnotation(SlidingWindowRateLimit.class);
        if (ann == null) {
            return pjp.proceed();
        }

        HttpServletRequest request = currentRequest();
        if (request == null) {
            return pjp.proceed();
        }

        String finalKey = buildLimitKey(ann, request);
        boolean ok = slidingWindowRateLimiter.tryAcquire(
                finalKey,
                ann.windowSeconds(),
                ann.maxRequests());

        if (!ok) {
            throw new RateLimitException(ann.message());
        }
        return pjp.proceed();
    }

    private HttpServletRequest currentRequest() {
        var attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes s) {
            return s.getRequest();
        }
        return null;
    }

    private String buildLimitKey(SlidingWindowRateLimit ann, HttpServletRequest request) {
        // 统一排序保证同一维度组合 key 稳定
        LimitDimension[] dims = ann.dimensions();
        if (dims == null || dims.length == 0) {
            dims = new LimitDimension[]{LimitDimension.GLOBAL};
        }
        String dimPart = Arrays.stream(dims)
                .sorted(Comparator.comparing(Enum::name))
                .map(d -> d.name() + ":" + resolveDimensionValue(d, request))
                .collect(Collectors.joining("|"));
        return "rate:" + ann.key() + ":" + dimPart;
    }

    private String resolveDimensionValue(LimitDimension d, HttpServletRequest request) {
        return switch (d) {
            case GLOBAL -> "1";
            case IP -> clientKeyResolver.resolveIp(request);
            case USER -> clientKeyResolver.resolveUserId(request);
        };
    }
}

