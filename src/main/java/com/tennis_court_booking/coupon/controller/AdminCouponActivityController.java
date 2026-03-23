package com.tennis_court_booking.coupon.controller;

import com.tennis_court_booking.coupon.mapper.CouponActivityMapper;
import com.tennis_court_booking.coupon.pojo.CouponActivity;
import com.tennis_court_booking.coupon.service.CouponSeckillService;
import com.tennis_court_booking.pojo.vo.Result;
import com.tennis_court_booking.websocket.BookingNotificationPublisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理员：创建秒杀活动、发布并初始化 Redis 库存。
 */
@RestController
@RequestMapping("/api/admin/coupon-activities")
public class AdminCouponActivityController {

    @Autowired
    private CouponActivityMapper couponActivityMapper;

    @Autowired
    private CouponSeckillService couponSeckillService;

    @Autowired
    private BookingNotificationPublisher bookingNotificationPublisher;

    /**
     * 活动列表（含草稿），供管理端页面展示。
     */
    @GetMapping
    public Result<List<CouponActivity>> list() {
        return Result.success(couponActivityMapper.listAll());
    }

    public record CreateBody(
            String title,
            Integer totalStock,
            LocalDateTime startTime,
            LocalDateTime endTime,
            BigDecimal discountAmount
    ) {}

    @PostMapping
    public Result<Integer> create(@RequestBody CreateBody body) {
        if (body.title() == null || body.title().isBlank()) {
            return Result.error(400, "标题不能为空");
        }
        if (body.totalStock() == null || body.totalStock() < 1) {
            return Result.error(400, "库存必须大于 0");
        }
        if (body.startTime() == null || body.endTime() == null || !body.endTime().isAfter(body.startTime())) {
            return Result.error(400, "时间范围无效");
        }
        CouponActivity a = new CouponActivity();
        a.setTitle(body.title().trim());
        a.setTotalStock(body.totalStock());
        a.setStartTime(body.startTime());
        a.setEndTime(body.endTime());
        a.setDiscountAmount(body.discountAmount() != null ? body.discountAmount() : BigDecimal.ZERO);
        a.setStatus(0);
        couponActivityMapper.insert(a);
        couponSeckillService.evictActiveActivitiesCache();
        return Result.success(a.getId());
    }

    /**
     * 发布活动：状态改为已发布，并将库存写入 Redis（与 total_stock 一致）。
     */
    @PutMapping("/{id}/publish")
    public Result<Void> publish(@PathVariable Integer id) {
        CouponActivity act = couponActivityMapper.findById(id);
        if (act == null) {
            return Result.error(404, "活动不存在");
        }
        if (act.getStatus() != null && act.getStatus() != 0) {
            return Result.error(400, "仅草稿可发布");
        }
        couponActivityMapper.updateStatus(id, 1);
        couponSeckillService.evictActiveActivitiesCache();
        couponSeckillService.initRedisStock(id, act.getTotalStock());
        try {
            bookingNotificationPublisher.notifyCouponActivityPublished(id, act.getTitle());
        } catch (Exception ignored) {
            // 广播失败不影响发布成功
        }
        return Result.success();
    }
}
