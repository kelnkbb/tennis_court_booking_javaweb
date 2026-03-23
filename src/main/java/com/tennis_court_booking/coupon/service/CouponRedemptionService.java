package com.tennis_court_booking.coupon.service;

import com.tennis_court_booking.coupon.mapper.CouponActivityMapper;
import com.tennis_court_booking.coupon.mapper.CouponUserRecordMapper;
import com.tennis_court_booking.coupon.pojo.CouponActivity;
import com.tennis_court_booking.coupon.pojo.CouponUserRecord;
import com.tennis_court_booking.pojo.entity.Booking;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 预约下单时校验券码、计算抵扣与应付金额；成功后由 {@link #markCouponUsed(Long, Integer)} 扣减券状态。
 */
@Service
@RequiredArgsConstructor
public class CouponRedemptionService {

    private final CouponUserRecordMapper couponUserRecordMapper;
    private final CouponActivityMapper couponActivityMapper;

    /**
     * 根据可选券码填充 booking 的 couponCode / couponDiscount / payAmount；无券则 payAmount = totalAmount。
     *
     * @return 若使用了券，返回券记录主键，用于后续 markCouponUsed；否则 null
     */
    public Long applyCouponIfPresent(Booking booking, String couponCodeInput) {
        BigDecimal total = booking.getTotalAmount();
        if (total == null) {
            throw new IllegalArgumentException("订单金额异常");
        }
        if (!StringUtils.hasText(couponCodeInput)) {
            booking.setCouponCode(null);
            booking.setCouponDiscount(null);
            booking.setPayAmount(total);
            return null;
        }
        String code = couponCodeInput.trim();
        CouponUserRecord record = couponUserRecordMapper.findByCouponCodeAndUserId(code, booking.getUserId());
        if (record == null) {
            throw new IllegalArgumentException("券码不存在或不属于当前账号");
        }
        if (record.getUseStatus() != null && record.getUseStatus() != 0) {
            throw new IllegalArgumentException("该优惠券已使用");
        }
        CouponActivity act = couponActivityMapper.findById(record.getActivityId());
        if (act == null) {
            throw new IllegalArgumentException("活动不存在");
        }
        if (act.getStatus() == null || act.getStatus() != 1) {
            throw new IllegalArgumentException("优惠活动未开放");
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(act.getStartTime()) || now.isAfter(act.getEndTime())) {
            throw new IllegalArgumentException("不在该券可用时间范围内");
        }
        BigDecimal face = act.getDiscountAmount() != null ? act.getDiscountAmount() : BigDecimal.ZERO;
        if (face.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("该券面额无效");
        }
        BigDecimal discount = face.min(total);
        BigDecimal pay = total.subtract(discount);

        booking.setCouponCode(code);
        booking.setCouponDiscount(discount);
        booking.setPayAmount(pay);
        return record.getId();
    }

    public void markCouponUsed(Long recordId, Integer bookingId) {
        if (recordId == null) {
            return;
        }
        int n = couponUserRecordMapper.markUsedById(recordId, bookingId);
        if (n != 1) {
            throw new IllegalStateException("优惠券核销失败，请稍后重试");
        }
    }
}
