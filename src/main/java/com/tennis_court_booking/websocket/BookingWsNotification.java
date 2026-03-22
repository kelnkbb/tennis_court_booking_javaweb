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

    /** PAYMENT_VERIFY_APPROVED | PAYMENT_VERIFY_REJECTED | CANCEL_REQUEST_APPROVED | CANCEL_REQUEST_REJECTED */
    private String type;
    private Integer bookingId;
    private String title;
    private String message;
}
