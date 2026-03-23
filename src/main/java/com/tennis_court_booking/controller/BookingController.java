// com/tennis_court_booking/controller/BookingController.java
package com.tennis_court_booking.controller;

import com.tennis_court_booking.pojo.entity.Booking;
import com.tennis_court_booking.pojo.vo.BookingVO;
import com.tennis_court_booking.pojo.vo.CourtBookingStats;
import com.tennis_court_booking.pojo.vo.CourtSlotOptionsVO;
import com.tennis_court_booking.pojo.vo.Result;
import com.tennis_court_booking.service.BookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class BookingController {

    @Autowired
    private BookingService bookingService;

    /**
     * 获取所有预约列表
     */
    @GetMapping("/bookings")
    public Result<List<BookingVO>> getAllBookings() {
        List<BookingVO> bookings = bookingService.findAll();
        return Result.success(bookings);
    }

    /**
     * 条件查询预约
     */
    @GetMapping("/bookings/search")
    public Result<List<BookingVO>> searchBookings(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer courtId,
            @RequestParam(required = false) Integer userId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        List<BookingVO> bookings = bookingService.findByCondition(keyword, courtId, userId,
                status, startDate, endDate);
        return Result.success(bookings);
    }

    /**
     * 根据ID查询预约
     */
    @GetMapping("/bookings/{id}")
    public Result<BookingVO> getBooking(@PathVariable Integer id) {
        System.out.println("接收到查询预约请求，id = " + id);
        BookingVO booking = bookingService.findById(id);
        return Result.success(booking);
    }

    /**
     * 新增预约
     */
    @PostMapping("/bookings")
    public Result<Booking> addBooking(@RequestBody Booking booking) {
        System.out.println("接收到新增预约请求: " + booking);

        // 验证时间是否可用
        if (!bookingService.checkTimeAvailable(booking.getCourtId(), booking.getBookingDate(),
                booking.getStartTime(), booking.getEndTime(), null)) {
            return Result.error("该时间段已被预约，请选择其他时间");
        }

        try {
            Booking newBooking = bookingService.addBooking(booking);
            return Result.success("新增成功", newBooking);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.error(400, e.getMessage());
        }
    }

    /**
     * 更新预约
     */
    @PutMapping("/bookings/{id}")
    public Result<Booking> updateBooking(@PathVariable Integer id, @RequestBody Booking booking) {
        System.out.println("接收到更新预约请求，id = " + id + ", booking = " + booking);
        booking.setId(id);

        // 验证时间是否可用（排除自身）
        if (!bookingService.checkTimeAvailable(booking.getCourtId(), booking.getBookingDate(),
                booking.getStartTime(), booking.getEndTime(), id)) {
            return Result.error("该时间段已被预约，请选择其他时间");
        }

        Booking updatedBooking = bookingService.updateBooking(booking);
        return Result.success("更新成功", updatedBooking);
    }

    /**
     * 取消预约
     */
    @PutMapping("/bookings/{id}/cancel")
    public Result<String> cancelBooking(@PathVariable Integer id) {
        System.out.println("接收到取消预约请求，id = " + id);
        bookingService.cancelBooking(id);
        return Result.success("取消成功");
    }

    /**
     * 完成预约
     */
    @PutMapping("/bookings/{id}/complete")
    public Result<String> completeBooking(@PathVariable Integer id) {
        System.out.println("接收到完成预约请求，id = " + id);
        bookingService.completeBooking(id);
        return Result.success("操作成功");
    }

    /**
     * 更新预约状态
     */
    @PutMapping("/bookings/{id}/status")
    public Result<String> updateStatus(@PathVariable Integer id, @RequestParam Integer status) {
        System.out.println("接收到更新状态请求，id = " + id + ", status = " + status);
        bookingService.updateStatus(id, status);
        return Result.success("状态更新成功");
    }

    /**
     * 删除预约
     */
    @DeleteMapping("/bookings/{id}")
    public Result<String> deleteBooking(@PathVariable Integer id) {
        System.out.println("接收到删除预约请求，id = " + id);
        bookingService.deleteById(id);
        return Result.success("删除成功");
    }

    /**
     * 批量删除预约
     */
    @DeleteMapping("/bookings/batch")
    public Result<String> batchDeleteBookings(@RequestBody List<Integer> ids) {
        System.out.println("接收到批量删除预约请求，ids = " + ids);
        bookingService.batchDelete(ids);
        return Result.success("批量删除成功");
    }

    /**
     * 检查时间段是否可用
     */
    @GetMapping("/bookings/check-time")
    public Result<Boolean> checkTimeAvailable(
            @RequestParam Integer courtId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate bookingDate,
            @RequestParam String startTime,
            @RequestParam String endTime,
            @RequestParam(required = false) Integer excludeId) {

        LocalTime start = LocalTime.parse(startTime);
        LocalTime end = LocalTime.parse(endTime);

        boolean available = bookingService.checkTimeAvailable(courtId, bookingDate, start, end, excludeId);
        return Result.success(available);
    }

    /**
     * 获取场地预约统计
     */
    @GetMapping("/bookings/stats/court")
    public Result<List<CourtBookingStats>> getCourtBookingStats() {
        List<CourtBookingStats> stats = bookingService.getCourtBookingStats();
        return Result.success(stats);
    }

    /**
     * 获取今日预约数量
     */
    @GetMapping("/bookings/today-count")
    public Result<Integer> getTodayBookingCount() {
        int count = bookingService.getTodayBookingCount();
        return Result.success(count);
    }

    /**
     * 获取当前登录用户的预约总数
     */
    @GetMapping("/bookings/my-count")
    public Result<Integer> getMyBookingCount(@RequestAttribute("userId") Integer userId) {
        int count = bookingService.getBookingCountByUserId(userId);
        return Result.success(count);
    }

    /**
     * 当前登录用户的预约列表（我的预订）。按 JWT 中的 userId 查询，不依赖前端传参。
     */
    @GetMapping("/bookings/mine")
    public Result<List<BookingVO>> getMyBookings(@RequestAttribute("userId") Integer userId) {
        List<BookingVO> list = bookingService.findByCondition(null, null, userId, null, null, null);
        return Result.success(list);
    }

    /**
     * 某场地某日可选时段（整点 1h/连续 2h），每日全用户合计最多 2 小时
     */
    @GetMapping("/bookings/court-slot-options")
    public Result<CourtSlotOptionsVO> getCourtSlotOptions(
            @RequestParam Integer courtId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Integer excludeId) {
        CourtSlotOptionsVO vo = bookingService.getCourtSlotOptions(courtId, date, excludeId);
        return Result.success(vo);
    }

    /**
     * 用户申请取消（待付款订单），需管理员审核后才真正取消
     */
    @PutMapping("/bookings/{id}/request-cancel")
    public Result<String> userRequestCancel(
            @PathVariable Integer id,
            @RequestAttribute("userId") Integer userId) {
        int n = bookingService.userRequestCancel(id, userId);
        if (n == 0) {
            return Result.error(400, "无法提交（仅本人待付款订单可申请，且未在审核中）");
        }
        return Result.success("已提交取消申请，请等待管理员审核", null);
    }

    /**
     * 用户确认付款（微信/支付宝/闲鱼）。正式环境微信支付宝应改为支付回调后再改状态，见 PAYMENT_INTEGRATION.md
     */
    @PostMapping("/bookings/{id}/pay")
    public Result<String> userPay(
            @PathVariable Integer id,
            @RequestAttribute("userId") Integer userId,
            @RequestBody(required = false) Map<String, String> body) {
        String channel = body == null ? null : body.get("channel");
        int n = bookingService.userPayBooking(id, userId, channel);
        if (n == 0) {
            return Result.error(400, "无法付款（请确认待付款、支付方式正确，且未在取消/付款审核中）");
        }
        return Result.success("付款已确认", null);
    }

    /**
     * 用户线下扫码后点击「我已支付」，提交所选渠道，等待管理员审核通过后变为已付款
     */
    @PostMapping("/bookings/{id}/claim-paid")
    public Result<String> userClaimPaid(
            @PathVariable Integer id,
            @RequestAttribute("userId") Integer userId,
            @RequestBody(required = false) Map<String, String> body) {
        String channel = body == null ? null : body.get("channel");
        int n = bookingService.userClaimPaid(id, userId, channel);
        if (n == 0) {
            return Result.error(400, "无法提交（请确认本人待付款订单、支付方式正确，且未在取消审核中或付款审核中）");
        }
        return Result.success("已提交，请等待管理员确认收款", null);
    }
}