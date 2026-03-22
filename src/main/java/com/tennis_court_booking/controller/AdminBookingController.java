package com.tennis_court_booking.controller;

import com.tennis_court_booking.pojo.vo.BookingVO;
import com.tennis_court_booking.pojo.vo.Result;
import com.tennis_court_booking.service.BookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理员：预约相关（取消申请审核等）
 */
@RestController
@RequestMapping("/api/admin/bookings")
public class AdminBookingController {

    @Autowired
    private BookingService bookingService;

    @GetMapping("/pending-cancels")
    public Result<List<BookingVO>> listPendingCancelRequests() {
        return Result.success(bookingService.findBookingsPendingCancel());
    }

    @PutMapping("/{id}/cancel-request/approve")
    public Result<String> approveCancel(@PathVariable Integer id) {
        int n = bookingService.adminApproveCancel(id);
        if (n == 0) {
            return Result.error(400, "无待审核的取消申请或订单不存在");
        }
        return Result.success("已通过，预约已取消", null);
    }

    @PutMapping("/{id}/cancel-request/reject")
    public Result<String> rejectCancel(@PathVariable Integer id) {
        int n = bookingService.adminRejectCancel(id);
        if (n == 0) {
            return Result.error(400, "无待审核的取消申请或订单不存在");
        }
        return Result.success("已驳回，用户可继续付款或再次申请取消", null);
    }

    @GetMapping("/pending-payment-verifies")
    public Result<List<BookingVO>> listPendingPaymentVerifies() {
        return Result.success(bookingService.findBookingsPendingPaymentVerify());
    }

    @PutMapping("/{id}/payment-verify/approve")
    public Result<String> approvePaymentVerify(@PathVariable Integer id) {
        int n = bookingService.adminApprovePaymentVerify(id);
        if (n == 0) {
            return Result.error(400, "无待审核的付款确认或订单不存在");
        }
        return Result.success("已确认收款，订单已标记为已付款", null);
    }

    @PutMapping("/{id}/payment-verify/reject")
    public Result<String> rejectPaymentVerify(@PathVariable Integer id) {
        int n = bookingService.adminRejectPaymentVerify(id);
        if (n == 0) {
            return Result.error(400, "无待审核的付款确认或订单不存在");
        }
        return Result.success("已驳回，用户可修改后再次提交", null);
    }
}
