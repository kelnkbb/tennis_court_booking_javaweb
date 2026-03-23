package com.tennis_court_booking.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 管理员审核结果 → WebSocket 推送给对应用户。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookingNotificationPublisher {

    public static final String TYPE_PAYMENT_VERIFY_APPROVED = "PAYMENT_VERIFY_APPROVED";
    public static final String TYPE_PAYMENT_VERIFY_REJECTED = "PAYMENT_VERIFY_REJECTED";
    public static final String TYPE_CANCEL_REQUEST_APPROVED = "CANCEL_REQUEST_APPROVED";
    public static final String TYPE_CANCEL_REQUEST_REJECTED = "CANCEL_REQUEST_REJECTED";
    public static final String TYPE_COUPON_ACTIVITY_PUBLISHED = "COUPON_ACTIVITY_PUBLISHED";

    private final UserNotificationSessionRegistry sessionRegistry;
    private final ObjectMapper objectMapper;

    public void notifyPaymentVerifyResult(Integer userId, Integer bookingId, boolean approved) {
        BookingWsNotification n = approved
                ? new BookingWsNotification(TYPE_PAYMENT_VERIFY_APPROVED, bookingId, null,
                "付款已确认", "管理员已确认您的付款，订单 #" + bookingId + " 已标记为已付款。")
                : new BookingWsNotification(TYPE_PAYMENT_VERIFY_REJECTED, bookingId, null,
                "付款确认未通过", "管理员驳回了您的付款确认，请核对后重新提交。");
        send(userId, n);
    }

    public void notifyCancelRequestResult(Integer userId, Integer bookingId, boolean approved) {
        BookingWsNotification n = approved
                ? new BookingWsNotification(TYPE_CANCEL_REQUEST_APPROVED, bookingId, null,
                "取消申请已通过", "管理员已通过您的取消申请，预约 #" + bookingId + " 已取消。")
                : new BookingWsNotification(TYPE_CANCEL_REQUEST_REJECTED, bookingId, null,
                "取消申请已驳回", "管理员驳回了您的取消申请，您可继续付款或再次申请。");
        send(userId, n);
    }

    /**
     * 管理员发布秒杀活动后，通知所有在线用户（WebSocket）。
     */
    public void notifyCouponActivityPublished(Integer activityId, String activityTitle) {
        String title = "新秒杀优惠券";
        String message = "管理员已发布活动「" + (activityTitle != null ? activityTitle : "") + "」，可前往「优惠券秒杀」参与抢购。";
        BookingWsNotification n = new BookingWsNotification(
                TYPE_COUPON_ACTIVITY_PUBLISHED, null, activityId, title, message);
        broadcast(n);
    }

    private void broadcast(BookingWsNotification payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            sessionRegistry.broadcastJson(json);
        } catch (JsonProcessingException e) {
            log.error("WS 广播序列化失败", e);
        }
    }

    private void send(Integer userId, BookingWsNotification payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            sessionRegistry.sendJsonToUser(userId, json);
        } catch (JsonProcessingException e) {
            log.error("WS 通知序列化失败", e);
        }
    }
}
