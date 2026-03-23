package com.tennis_court_booking.coupon.controller;

import com.tennis_court_booking.coupon.async.SeckillGrabRedisResult;
import com.tennis_court_booking.coupon.mapper.CouponUserRecordMapper;
import com.tennis_court_booking.coupon.pojo.CouponActivity;
import com.tennis_court_booking.coupon.pojo.CouponUnusedVO;
import com.tennis_court_booking.coupon.service.CouponSeckillAsyncService;
import com.tennis_court_booking.coupon.service.CouponSeckillService;
import com.tennis_court_booking.limit.annotation.SlidingWindowRateLimit;
import com.tennis_court_booking.limit.model.LimitDimension;
import com.tennis_court_booking.pojo.vo.Result;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户端：查看进行中的秒杀活动、抢购（需登录）。
 */
@RestController
@RequestMapping("/api/coupons")
public class CouponSeckillController {

    @Autowired
    private CouponSeckillService couponSeckillService;

    @Autowired
    private CouponSeckillAsyncService couponSeckillAsyncService;

    @Autowired
    private CouponUserRecordMapper couponUserRecordMapper;

    /**
     * 当前可参与的秒杀活动列表（已开始且未结束、已发布）。
     */
    @SlidingWindowRateLimit(
            key = "coupon:seckill:activities",
            windowSeconds = 10,
            maxRequests = 30,
            dimensions = {LimitDimension.IP},
            message = "访问太频繁，请稍后再试"
    )
    @GetMapping("/activities")
    public Result<List<CouponActivity>> listActivities() {
        return Result.success(couponSeckillService.listActiveActivities());
    }

    /**
     * 当前用户未使用的秒杀券（下单抵扣可选）。
     */
    @GetMapping("/my-unused")
    public Result<List<CouponUnusedVO>> myUnused(HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("userId");
        if (userId == null) {
            return Result.error(401, "请先登录");
        }
        return Result.success(couponUserRecordMapper.listUnusedByUserId(userId));
    }

    /**
     * 抢购：活动校验通过后写入消息队列，立即返回 grabId；Redis 扣减与券记录在消费者中异步完成。
     */
    @SlidingWindowRateLimit(
            key = "coupon:seckill:grab",
            windowSeconds = 10,
            maxRequests = 5,
            dimensions = {LimitDimension.IP, LimitDimension.USER},
            message = "抢购太频繁，请稍后再试"
    )
    @PostMapping("/seckill/{activityId}/grab")
    public Result<Map<String, Object>> grab(HttpServletRequest request, @PathVariable Integer activityId) {
        Integer userId = (Integer) request.getAttribute("userId");
        if (userId == null) {
            return Result.error(401, "请先登录");
        }
        try {
            String grabId = couponSeckillAsyncService.submitGrab(activityId, userId);
            Map<String, Object> data = new HashMap<>(4);
            data.put("grabId", grabId);
            data.put("async", true);
            data.put("message", "已排队处理，请轮询查询结果");
            return Result.success("已受理", data);
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        } catch (IllegalStateException e) {
            return Result.error(503, e.getMessage());
        }
    }

    /**
     * 查询异步抢购结果（需登录且只能查本人）。
     */
    @SlidingWindowRateLimit(
            key = "coupon:seckill:grab-result",
            windowSeconds = 5,
            maxRequests = 10,
            dimensions = {LimitDimension.IP, LimitDimension.USER},
            message = "查询太频繁，请稍后再试"
    )
    @GetMapping("/seckill/result/{grabId}")
    public Result<SeckillGrabRedisResult> grabResult(HttpServletRequest request, @PathVariable String grabId) {
        Integer userId = (Integer) request.getAttribute("userId");
        if (userId == null) {
            return Result.error(401, "请先登录");
        }
        SeckillGrabRedisResult r = couponSeckillService.readGrabResult(grabId);
        if (r == null) {
            return Result.error(404, "结果不存在或已过期");
        }
        if (r.getUserId() == null || !r.getUserId().equals(userId)) {
            return Result.error(403, "无权查看该结果");
        }
        return Result.success(r);
    }
}
