// com/tennis_court_booking/service/BookingService.java
package com.tennis_court_booking.service;

import com.tennis_court_booking.pojo.entity.Booking;
import com.tennis_court_booking.pojo.vo.BookingVO;
import com.tennis_court_booking.pojo.vo.CourtBookingStats;
import com.tennis_court_booking.pojo.vo.CourtSlotOptionsVO;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface BookingService {

    /**
     * 查询所有预约
     */
    List<BookingVO> findAll();

    /**
     * 根据ID查询预约
     */
    BookingVO findById(Integer id);

    /**
     * 条件查询预约
     */
    List<BookingVO> findByCondition(String keyword, Integer courtId, Integer userId,
                                    Integer status, LocalDate startDate, LocalDate endDate);

    /**
     * 新增预约
     */
    Booking addBooking(Booking booking);

    /**
     * 更新预约
     */
    Booking updateBooking(Booking booking);

    /**
     * 更新预约状态
     */
    void updateStatus(Integer id, Integer status);

    /**
     * 取消预约
     */
    void cancelBooking(Integer id);

    /**
     * 完成预约
     */
    void completeBooking(Integer id);

    /**
     * 删除预约
     */
    void deleteById(Integer id);

    /**
     * 批量删除预约
     */
    void batchDelete(List<Integer> ids);

    /**
     * 检查时间段是否可用
     */
    boolean checkTimeAvailable(Integer courtId, LocalDate bookingDate,
                               LocalTime startTime, LocalTime endTime, Integer excludeId);

    /**
     * 获取今日预约数量
     */
    int getTodayBookingCount();

    /**
     * 获取指定用户的预约总数
     */
    int getBookingCountByUserId(Integer userId);

    /**
     * 获取场地预约统计
     */
    List<CourtBookingStats> getCourtBookingStats();

    /**
     * 生成预约单号
     */
    String generateBookingNo();

    /**
     * 某场地某日可选时段（整点 1 小时 / 连续 2 小时），且全用户当日合计不超过 2 小时
     */
    CourtSlotOptionsVO getCourtSlotOptions(Integer courtId, LocalDate date, Integer excludeBookingId);

    /** 用户申请取消（待付款），需管理员审核 */
    int userRequestCancel(Integer bookingId, Integer userId);

    int userPayBooking(Integer bookingId, Integer userId, String channel);

    List<BookingVO> findBookingsPendingCancel();

    int adminApproveCancel(Integer bookingId);

    int adminRejectCancel(Integer bookingId);
}