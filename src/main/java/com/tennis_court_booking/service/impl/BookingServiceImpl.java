// com/tennis_court_booking/service/impl/BookingServiceImpl.java
package com.tennis_court_booking.service.impl;

import com.tennis_court_booking.mapper.BookingMapper;
import com.tennis_court_booking.mapper.CourtMapper;
import com.tennis_court_booking.pojo.entity.Booking;
import com.tennis_court_booking.pojo.entity.Court;
import com.tennis_court_booking.pojo.vo.BookingVO;
import com.tennis_court_booking.pojo.vo.CourtBookingStats;
import com.tennis_court_booking.pojo.vo.CourtSlotOptionsVO;
import com.tennis_court_booking.pojo.vo.SlotOptionVO;
import com.tennis_court_booking.service.BookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@Transactional
public class BookingServiceImpl implements BookingService {

    /** 同一场地、同一自然日，所有用户预约时长合计上限（小时） */
    public static final BigDecimal MAX_DAILY_HOURS_PER_COURT = new BigDecimal("2");

    private static final DateTimeFormatter HM = DateTimeFormatter.ofPattern("HH:mm");

    @Autowired
    private BookingMapper bookingMapper;

    @Autowired
    private CourtMapper courtMapper;

    @Override
    public List<BookingVO> findAll() {
        return bookingMapper.findAll();
    }

    @Override
    public BookingVO findById(Integer id) {
        return bookingMapper.findById(id);
    }

    @Override
    public List<BookingVO> findByCondition(String keyword, Integer courtId, Integer userId,
                                           Integer status, LocalDate startDate, LocalDate endDate) {
        return bookingMapper.findByCondition(keyword, courtId, userId, status, startDate, endDate);
    }

    @Override
    public Booking addBooking(Booking booking) {
        System.out.println("Service层新增预约: " + booking);

        // 生成预约单号
        if (booking.getBookingNo() == null || booking.getBookingNo().isEmpty()) {
            booking.setBookingNo(generateBookingNo());
        }

        // 设置创建和更新时间
        booking.setCreateTime(LocalDateTime.now());
        booking.setUpdateTime(LocalDateTime.now());

        // 计算时长（小时）
        if (booking.getStartTime() != null && booking.getEndTime() != null) {
            long minutes = java.time.Duration.between(booking.getStartTime(), booking.getEndTime()).toMinutes();
            BigDecimal hours = BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 2, BigDecimal.ROUND_HALF_UP);
            booking.setDuration(hours);
        }

        // 获取场地单价并计算总金额
        if (booking.getCourtId() != null && booking.getDuration() != null) {
            Court court = courtMapper.getCourt(booking.getCourtId());
            if (court != null) {
                booking.setUnitPrice(court.getPrice());
                if (booking.getUnitPrice() != null) {
                    booking.setTotalAmount(booking.getUnitPrice().multiply(booking.getDuration()));
                }
            }
        }

        // 设置默认状态为待付款
        if (booking.getStatus() == null) {
            booking.setStatus(1); // 待付款
        }

        // 检查时间冲突
        if (!checkTimeAvailable(booking.getCourtId(), booking.getBookingDate(),
                booking.getStartTime(), booking.getEndTime(), null)) {
            throw new RuntimeException("该时间段已被预约，请选择其他时间");
        }

        // 每日全用户合计不超过 2 小时
        BigDecimal usedOthers = bookingMapper.sumDurationForCourtDate(
                booking.getCourtId(), booking.getBookingDate(), null);
        if (usedOthers == null) {
            usedOthers = BigDecimal.ZERO;
        }
        if (booking.getDuration() != null
                && usedOthers.add(booking.getDuration()).compareTo(MAX_DAILY_HOURS_PER_COURT) > 0) {
            throw new RuntimeException("该日该场地可预约总时长已达上限（每日最多2小时，含所有用户）");
        }

        if (booking.getCancelRequestStatus() == null) {
            booking.setCancelRequestStatus(0);
        }

        bookingMapper.addBooking(booking);
        System.out.println("新增成功，生成ID: " + booking.getId());

        return booking;
    }

    @Override
    public Booking updateBooking(Booking booking) {
        System.out.println("Service层更新预约: " + booking);

        BookingVO existingVo = bookingMapper.findById(booking.getId());
        if (existingVo != null) {
            booking.setCancelRequestStatus(
                    existingVo.getCancelRequestStatus() != null ? existingVo.getCancelRequestStatus() : 0);
            booking.setPaymentChannel(existingVo.getPaymentChannel());
            booking.setPayTime(existingVo.getPayTime());
        }

        // 设置更新时间
        booking.setUpdateTime(LocalDateTime.now());

        // 重新计算时长
        if (booking.getStartTime() != null && booking.getEndTime() != null) {
            long minutes = java.time.Duration.between(booking.getStartTime(), booking.getEndTime()).toMinutes();
            BigDecimal hours = BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 2, BigDecimal.ROUND_HALF_UP);
            booking.setDuration(hours);
        }

        // 重新计算总金额
        if (booking.getCourtId() != null && booking.getDuration() != null) {
            Court court = courtMapper.getCourt(booking.getCourtId());
            if (court != null) {
                booking.setUnitPrice(court.getPrice());
                if (booking.getUnitPrice() != null) {
                    booking.setTotalAmount(booking.getUnitPrice().multiply(booking.getDuration()));
                }
            }
        }

        // 检查时间冲突（排除当前预约）
        if (!checkTimeAvailable(booking.getCourtId(), booking.getBookingDate(),
                booking.getStartTime(), booking.getEndTime(), booking.getId())) {
            throw new RuntimeException("该时间段已被预约，请选择其他时间");
        }

        BigDecimal usedOthers = bookingMapper.sumDurationForCourtDate(
                booking.getCourtId(), booking.getBookingDate(), booking.getId());
        if (usedOthers == null) {
            usedOthers = BigDecimal.ZERO;
        }
        if (booking.getDuration() != null
                && usedOthers.add(booking.getDuration()).compareTo(MAX_DAILY_HOURS_PER_COURT) > 0) {
            throw new RuntimeException("该日该场地可预约总时长已达上限（每日最多2小时，含所有用户）");
        }

        bookingMapper.updateBooking(booking);
        System.out.println("更新成功，ID: " + booking.getId());

        return booking;
    }

    @Override
    public void updateStatus(Integer id, Integer status) {
        bookingMapper.updateStatus(id, status);
    }

    @Override
    public void cancelBooking(Integer id) {
        bookingMapper.updateStatus(id, 0); // 0-已取消
    }

    @Override
    public void completeBooking(Integer id) {
        bookingMapper.updateStatus(id, 3); // 3-已完成
    }

    @Override
    public void deleteById(Integer id) {
        bookingMapper.deleteById(id);
    }

    @Override
    public void batchDelete(List<Integer> ids) {
        bookingMapper.batchDelete(ids);
    }

    @Override
    public boolean checkTimeAvailable(Integer courtId, LocalDate bookingDate,
                                      LocalTime startTime, LocalTime endTime, Integer excludeId) {
        int count = bookingMapper.checkTimeConflict(courtId, bookingDate, startTime, endTime, excludeId);
        return count == 0;
    }

    @Override
    public int getTodayBookingCount() {
        return bookingMapper.getTodayBookingCount();
    }

    @Override
    public int getBookingCountByUserId(Integer userId) {
        return bookingMapper.getBookingCountByUserId(userId);
    }

    @Override
    public List<CourtBookingStats> getCourtBookingStats() {
        return bookingMapper.getCourtBookingStats();
    }

    @Override
    public String generateBookingNo() {
        // 格式：BK + 年月日 + 6位随机数
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomStr = String.format("%06d", (int) (Math.random() * 1000000));
        return "BK" + dateStr + randomStr;
    }

    @Override
    public CourtSlotOptionsVO getCourtSlotOptions(Integer courtId, LocalDate date, Integer excludeBookingId) {
        CourtSlotOptionsVO vo = new CourtSlotOptionsVO();
        vo.setMaxDailyHours(MAX_DAILY_HOURS_PER_COURT);

        Court court = courtMapper.getCourt(courtId);
        if (court == null) {
            vo.setUsedHours(BigDecimal.ZERO);
            vo.setRemainingHours(BigDecimal.ZERO);
            vo.setDayFull(true);
            vo.setOpenTimeDisplay("—");
            return vo;
        }

        vo.setOpenTimeDisplay(court.getOpenTime() != null ? court.getOpenTime() : "09:00-22:00");

        BigDecimal used = bookingMapper.sumDurationForCourtDate(courtId, date, excludeBookingId);
        if (used == null) {
            used = BigDecimal.ZERO;
        }
        vo.setUsedHours(used);
        vo.setRemainingHours(MAX_DAILY_HOURS_PER_COURT.subtract(used).max(BigDecimal.ZERO));
        vo.setDayFull(used.compareTo(MAX_DAILY_HOURS_PER_COURT) >= 0);

        if (court.getStatus() == null || court.getStatus() != 1) {
            vo.setCourtClosed(true);
            return vo;
        }
        if (vo.isDayFull()) {
            return vo;
        }

        LocalTime[] open = parseOpenTime(court.getOpenTime());
        LocalTime openStart = open[0];
        LocalTime openEnd = open[1];
        if (!openEnd.isAfter(openStart)) {
            return vo;
        }

        List<BookingVO> ranges = bookingMapper.findActiveTimeRangesForCourtDate(courtId, date, excludeBookingId);
        List<SlotOptionVO> options = new ArrayList<>();

        // 按营业时间起点，以 1 小时为步长，支持 08:30 这类非整点
        LocalTime slotStart = openStart;
        while (true) {
            LocalTime slotEnd = slotStart.plusHours(1);
            if (!slotEnd.isAfter(openEnd)) {
                if (!hasOverlapWithRanges(slotStart, slotEnd, ranges)) {
                    BigDecimal after = used.add(BigDecimal.ONE);
                    if (after.compareTo(MAX_DAILY_HOURS_PER_COURT) <= 0) {
                        String s = slotStart.format(HM);
                        String e = slotEnd.format(HM);
                        options.add(new SlotOptionVO(s + "|" + e, s + " - " + e + "（1小时）", s, e, 1));
                    }
                }
                slotStart = slotStart.plusHours(1);
            } else {
                break;
            }
        }

        options.sort(Comparator.comparing(SlotOptionVO::getStartTime).thenComparingInt(SlotOptionVO::getHours));
        vo.setOptions(options);
        return vo;
    }

    private static boolean hasOverlapWithRanges(LocalTime aStart, LocalTime aEnd, List<BookingVO> ranges) {
        for (BookingVO b : ranges) {
            if (b.getStartTime() == null || b.getEndTime() == null) {
                continue;
            }
            if (aStart.isBefore(b.getEndTime()) && aEnd.isAfter(b.getStartTime())) {
                return true;
            }
        }
        return false;
    }

    private static LocalTime[] parseOpenTime(String openTime) {
        try {
            if (openTime != null && openTime.contains("-")) {
                String[] p = openTime.trim().split("-", 2);
                return new LocalTime[]{
                        LocalTime.parse(p[0].trim()),
                        LocalTime.parse(p[1].trim())
                };
            }
        } catch (Exception ignored) {
            // fall through
        }
        return new LocalTime[]{LocalTime.of(9, 0), LocalTime.of(22, 0)};
    }

    @Override
    public int userRequestCancel(Integer bookingId, Integer userId) {
        return bookingMapper.userRequestCancelBooking(bookingId, userId);
    }

    @Override
    public int userPayBooking(Integer bookingId, Integer userId, String channel) {
        if (channel == null) {
            return 0;
        }
        String c = channel.trim().toLowerCase();
        if (!c.equals("wechat") && !c.equals("alipay") && !c.equals("xianyu")) {
            return 0;
        }
        return bookingMapper.userPayBooking(bookingId, userId, c);
    }

    @Override
    public List<BookingVO> findBookingsPendingCancel() {
        return bookingMapper.findBookingsPendingCancel();
    }

    @Override
    public int adminApproveCancel(Integer bookingId) {
        return bookingMapper.adminApproveCancelBooking(bookingId);
    }

    @Override
    public int adminRejectCancel(Integer bookingId) {
        return bookingMapper.adminRejectCancelBooking(bookingId);
    }
}