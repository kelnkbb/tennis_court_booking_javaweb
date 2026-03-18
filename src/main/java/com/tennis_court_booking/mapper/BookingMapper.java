// com/tennis_court_booking/mapper/BookingMapper.java
package com.tennis_court_booking.mapper;

import com.tennis_court_booking.pojo.entity.Booking;
import com.tennis_court_booking.pojo.vo.BookingVO;
import com.tennis_court_booking.pojo.vo.CourtBookingStats;
import com.tennis_court_booking.pojo.vo.CourtStatsVO;
import com.tennis_court_booking.pojo.vo.UserBookingStatsVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.time.LocalTime;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Mapper
public interface BookingMapper {

    /**
     * 查询所有预约（关联场地和用户信息）
     */
    List<BookingVO> findAll();

    /**
     * 根据ID查询预约
     */
    BookingVO findById(@Param("id") Integer id);

    /**
     * 根据预约单号查询
     */
    BookingVO findByBookingNo(@Param("bookingNo") String bookingNo);

    /**
     * 根据条件查询预约
     */
    List<BookingVO> findByCondition(@Param("keyword") String keyword,
                                    @Param("courtId") Integer courtId,
                                    @Param("userId") Integer userId,
                                    @Param("status") Integer status,
                                    @Param("startDate") LocalDate startDate,
                                    @Param("endDate") LocalDate endDate);

    /**
     * 检查时间段是否已被预约
     */
    int checkTimeConflict(@Param("courtId") Integer courtId,
                          @Param("bookingDate") LocalDate bookingDate,
                          @Param("startTime") LocalTime startTime,
                          @Param("endTime") LocalTime endTime,
                          @Param("excludeId") Integer excludeId);

    /**
     * 新增预约
     */
    void addBooking(Booking booking);

    /**
     * 更新预约
     */
    void updateBooking(Booking booking);

    /**
     * 更新预约状态
     */
    void updateStatus(@Param("id") Integer id, @Param("status") Integer status);

    /**
     * 删除预约
     */
    void deleteById(@Param("id") Integer id);

    /**
     * 批量删除预约
     */
    void batchDelete(@Param("ids") List<Integer> ids);

    /**
     * 获取今日预约数量
     */
    int getTodayBookingCount();

    /**
     * 获取场地预约统计
     */
    List<CourtBookingStats> getCourtBookingStats();

    /**
     * 管理员：按日期范围统计场地（start/end 都可为空；为空则不加该边界）
     */
    List<CourtStatsVO> getCourtStats(@Param("startDate") LocalDate startDate,
                                     @Param("endDate") LocalDate endDate);

    /**
     * 管理员：按日期范围统计用户（start/end 都可为空；为空则不加该边界）
     */
    List<UserBookingStatsVO> getUserBookingStats(@Param("startDate") LocalDate startDate,
                                                 @Param("endDate") LocalDate endDate);

    /**
     * 根据用户ID统计其预约总数
     */
    int getBookingCountByUserId(@Param("userId") Integer userId);

    /**
     * 某场地某日有效预约（待付款/已付款/已完成）总时长
     */
    BigDecimal sumDurationForCourtDate(@Param("courtId") Integer courtId,
                                       @Param("bookingDate") LocalDate bookingDate,
                                       @Param("excludeId") Integer excludeId);

    /**
     * 某场地某日有效预约的起止时间（用于时段冲突）
     */
    List<BookingVO> findActiveTimeRangesForCourtDate(@Param("courtId") Integer courtId,
                                                   @Param("bookingDate") LocalDate bookingDate,
                                                   @Param("excludeId") Integer excludeId);

    int userRequestCancelBooking(@Param("id") Integer id, @Param("userId") Integer userId);

    int adminApproveCancelBooking(@Param("id") Integer id);

    int adminRejectCancelBooking(@Param("id") Integer id);

    int userPayBooking(@Param("id") Integer id, @Param("userId") Integer userId, @Param("channel") String channel);

    List<BookingVO> findBookingsPendingCancel();
}