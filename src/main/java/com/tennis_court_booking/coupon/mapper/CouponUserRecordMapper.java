package com.tennis_court_booking.coupon.mapper;

import com.tennis_court_booking.coupon.pojo.CouponUserRecord;
import com.tennis_court_booking.coupon.pojo.CouponUnusedVO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface CouponUserRecordMapper {

    @Insert("""
            INSERT INTO coupon_user_record (activity_id, user_id, coupon_code, use_status)
            VALUES (#{activityId}, #{userId}, #{couponCode}, 0)
            """)
    int insert(CouponUserRecord record);

    @Select("""
            SELECT COUNT(1) FROM coupon_user_record
            WHERE activity_id = #{activityId} AND user_id = #{userId}
            """)
    int countByActivityAndUser(@Param("activityId") Integer activityId, @Param("userId") Integer userId);

    @Select("SELECT * FROM coupon_user_record WHERE coupon_code = #{code} AND user_id = #{userId}")
    CouponUserRecord findByCouponCodeAndUserId(@Param("code") String code, @Param("userId") Integer userId);

    @Select("SELECT * FROM coupon_user_record WHERE activity_id = #{activityId} AND user_id = #{userId} LIMIT 1")
    CouponUserRecord findByActivityIdAndUserId(@Param("activityId") Integer activityId, @Param("userId") Integer userId);

    @Update("""
            UPDATE coupon_user_record
            SET use_status = 1, booking_id = #{bookingId}
            WHERE id = #{id} AND use_status = 0
            """)
    int markUsedById(@Param("id") Long id, @Param("bookingId") Integer bookingId);

    @Select("""
            SELECT r.id, r.coupon_code AS couponCode, a.title AS activityTitle, a.discount_amount AS discountAmount
            FROM coupon_user_record r
            INNER JOIN coupon_activity a ON r.activity_id = a.id
            WHERE r.user_id = #{userId} AND r.use_status = 0
            ORDER BY r.grab_time DESC
            """)
    List<CouponUnusedVO> listUnusedByUserId(@Param("userId") Integer userId);
}
