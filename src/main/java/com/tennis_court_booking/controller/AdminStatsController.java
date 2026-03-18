package com.tennis_court_booking.controller;

import com.tennis_court_booking.pojo.vo.CourtStatsVO;
import com.tennis_court_booking.pojo.vo.Result;
import com.tennis_court_booking.pojo.vo.UserBookingStatsVO;
import com.tennis_court_booking.service.BookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * 管理员：统计报表
 */
@RestController
@RequestMapping("/api/admin/stats")
public class AdminStatsController {

    @Autowired
    private BookingService bookingService;

    /**
     * 用户统计（按预约日期过滤；start/end 为空则不限制）
     */
    @GetMapping("/users")
    public Result<List<UserBookingStatsVO>> userStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return Result.success(bookingService.getUserBookingStats(startDate, endDate));
    }

    /**
     * 场地统计（按预约日期过滤；start/end 为空则不限制）
     */
    @GetMapping("/courts")
    public Result<List<CourtStatsVO>> courtStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return Result.success(bookingService.getCourtStats(startDate, endDate));
    }
}

