package com.tennis_court_booking.coupon.mapper;

import com.tennis_court_booking.coupon.pojo.CouponActivity;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface CouponActivityMapper {

    @Insert("""
            INSERT INTO coupon_activity (title, total_stock, start_time, end_time, discount_amount, status)
            VALUES (#{title}, #{totalStock}, #{startTime}, #{endTime}, #{discountAmount}, #{status})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(CouponActivity activity);

    @Select("SELECT * FROM coupon_activity WHERE id = #{id}")
    CouponActivity findById(@Param("id") Integer id);

    @Update("UPDATE coupon_activity SET status = #{status}, update_time = NOW() WHERE id = #{id}")
    int updateStatus(@Param("id") Integer id, @Param("status") Integer status);

    @Select("""
            SELECT * FROM coupon_activity
            WHERE status = 1 AND start_time <= NOW() AND end_time >= NOW()
            ORDER BY start_time ASC
            """)
    List<CouponActivity> listActivePublished();

    @Select("SELECT * FROM coupon_activity ORDER BY id DESC")
    List<CouponActivity> listAll();
}
