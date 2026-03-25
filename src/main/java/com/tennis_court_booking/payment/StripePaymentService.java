package com.tennis_court_booking.payment;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import com.tennis_court_booking.mapper.BookingMapper;
import com.tennis_court_booking.payment.vo.StripeCheckoutVO;
import com.tennis_court_booking.pojo.vo.BookingVO;
import com.tennis_court_booking.service.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class StripePaymentService {

    private final StripeProperties stripeProperties;
    private final BookingMapper bookingMapper;
    private final BookingService bookingService;

    /**
     * 创建 Stripe Checkout 会话，前端跳转 {@link StripeCheckoutVO#getUrl()} 完成支付。
     */
    public StripeCheckoutVO createCheckoutSession(Integer bookingId, Integer userId) throws StripeException {
        if (!stripeProperties.isEnabled()) {
            throw new IllegalStateException("Stripe 未配置（请设置 stripe.secret-key）");
        }
        if (bookingId == null || userId == null) {
            throw new IllegalArgumentException("参数无效");
        }
        BookingVO vo = bookingMapper.findById(bookingId);
        if (vo == null) {
            throw new IllegalArgumentException("订单不存在");
        }
        if (vo.getUserId() == null || !vo.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权为该订单发起支付");
        }
        if (vo.getStatus() == null || vo.getStatus() != 1) {
            throw new IllegalArgumentException("仅待付款订单可在线支付");
        }

        BigDecimal pay = vo.getPayAmount() != null ? vo.getPayAmount() : vo.getTotalAmount();
        if (pay == null || pay.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("应付金额无效，无法发起 Stripe 支付");
        }

        String currency = stripeProperties.getCurrency() != null
                ? stripeProperties.getCurrency().trim().toLowerCase()
                : "cny";
        long unitAmountMinor = toMinorUnits(pay, currency);
        if (unitAmountMinor <= 0) {
            throw new IllegalArgumentException("金额过小，无法发起支付");
        }

        String bookingNo = vo.getBookingNo() != null ? vo.getBookingNo() : ("#" + bookingId);
        String success = appendSessionIdPlaceholder(stripeProperties.getSuccessUrl());
        String cancel = stripeProperties.getCancelUrl();

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(success)
                .setCancelUrl(cancel)
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setQuantity(1L)
                                .setPriceData(
                                        SessionCreateParams.LineItem.PriceData.builder()
                                                .setCurrency(currency)
                                                .setUnitAmount(unitAmountMinor)
                                                .setProductData(
                                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                .setName("网球场地预约 · " + bookingNo)
                                                                .build())
                                                .build())
                                .build())
                .putMetadata("bookingId", String.valueOf(bookingId))
                .putMetadata("userId", String.valueOf(userId))
                .putMetadata("currency", currency)
                .build();

        Session session = Session.create(params);
        return new StripeCheckoutVO(session.getUrl(), session.getId());
    }

    /**
     * 处理 Webhook：校验签名后，在支付成功时落库为已付款（渠道 stripe）。
     *
     * @return false 仅当签名校验失败或未配置密钥（应返回 HTTP 4xx）；其余情况返回 true（HTTP 200，避免 Stripe 无意义重试）
     */
    public boolean handleWebhookPayload(String payload, String sigHeader) {
        if (stripeProperties.getWebhookSecret() == null || stripeProperties.getWebhookSecret().isBlank()) {
            log.warn("Stripe Webhook 未配置 webhook-secret，拒绝处理");
            return false;
        }
        if (sigHeader == null || sigHeader.isBlank()) {
            return false;
        }
        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, stripeProperties.getWebhookSecret());
        } catch (SignatureVerificationException e) {
            log.warn("Stripe Webhook 签名校验失败: {}", e.getMessage());
            return false;
        }

        if (!"checkout.session.completed".equals(event.getType())) {
            return true;
        }

        Optional<StripeObject> opt = event.getDataObjectDeserializer().getObject();
        if (opt.isEmpty() || !(opt.get() instanceof Session)) {
            log.warn("Stripe Webhook 无法反序列化 Session");
            return true;
        }
        Session session = (Session) opt.get();
        if (!"paid".equals(session.getPaymentStatus())) {
            log.info("Stripe Session 未支付完成，忽略 sessionId={}", session.getId());
            return true;
        }

        var meta = session.getMetadata();
        if (meta == null) {
            log.warn("Stripe Session 无 metadata");
            return true;
        }
        String bid = meta.get("bookingId");
        String uid = meta.get("userId");
        if (bid == null || uid == null) {
            log.warn("Stripe Session metadata 缺少 bookingId/userId");
            return true;
        }
        int bookingId;
        int userId;
        try {
            bookingId = Integer.parseInt(bid.trim());
            userId = Integer.parseInt(uid.trim());
        } catch (NumberFormatException e) {
            log.warn("Stripe metadata 非数字: bookingId={} userId={}", bid, uid);
            return true;
        }

        BookingVO vo = bookingMapper.findById(bookingId);
        if (vo == null) {
            log.warn("Webhook 对应订单不存在 bookingId={}", bookingId);
            return true;
        }
        if (vo.getUserId() == null || !vo.getUserId().equals(userId)) {
            log.warn("Webhook userId 与订单不一致 bookingId={}", bookingId);
            return true;
        }

        BigDecimal pay = vo.getPayAmount() != null ? vo.getPayAmount() : vo.getTotalAmount();
        if (pay == null) {
            log.warn("订单金额为空 bookingId={}", bookingId);
            return true;
        }
        String currency = stripeProperties.getCurrency() != null
                ? stripeProperties.getCurrency().trim().toLowerCase()
                : "cny";
        long expectedMinor = toMinorUnits(pay, currency);
        Long total = session.getAmountTotal();
        String sessionCurrency = session.getCurrency() != null ? session.getCurrency().toLowerCase() : "";
        if (!currency.equals(sessionCurrency)) {
            log.warn("币种不一致 expected={} session={}", currency, sessionCurrency);
            return true;
        }
        if (total == null || total != expectedMinor) {
            log.warn("支付金额与订单不一致 bookingId={} expectedMinor={} sessionTotal={}", bookingId, expectedMinor, total);
            return true;
        }

        int n = bookingService.userPayBooking(bookingId, userId, "stripe");
        if (n > 0) {
            log.info("Stripe 支付已确认 bookingId={} sessionId={}", bookingId, session.getId());
        } else {
            log.info("订单未更新（可能已付款）bookingId={}", bookingId);
        }
        return true;
    }

    private static String appendSessionIdPlaceholder(String url) {
        if (url == null || url.isBlank()) {
            return "http://localhost:5173/my-bookings?stripe=success&session_id={CHECKOUT_SESSION_ID}";
        }
        if (url.contains("{CHECKOUT_SESSION_ID}")) {
            return url;
        }
        if (url.contains("?")) {
            return url + "&session_id={CHECKOUT_SESSION_ID}";
        }
        return url + "?session_id={CHECKOUT_SESSION_ID}";
    }

    /**
     * 订单金额为「元」时，转为 Stripe 最小货币单位（如 cny 为分）。
     */
    private static long toMinorUnits(BigDecimal amountYuan, String currency) {
        String c = currency.toLowerCase();
        int fractionDigits = 2;
        if ("jpy".equals(c) || "krw".equals(c) || "vnd".equals(c)) {
            fractionDigits = 0;
        }
        BigDecimal multiplier = BigDecimal.TEN.pow(fractionDigits);
        return amountYuan.multiply(multiplier).setScale(0, RoundingMode.HALF_UP).longValueExact();
    }
}
