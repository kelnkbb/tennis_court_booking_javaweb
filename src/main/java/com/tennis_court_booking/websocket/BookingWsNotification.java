package com.tennis_court_booking.websocket;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 推送给前端的预约相关通知（JSON）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingWsNotification {

    /** PAYMENT_VERIFY_* | CANCEL_REQUEST_* | COUPON_ACTIVITY_PUBLISHED */
    private String type;
    private Integer bookingId;
    /** 秒杀活动 ID（仅 COUPON_ACTIVITY_PUBLISHED 时有值） */
    private Integer activityId;
    private String title;
    private String message;
}
