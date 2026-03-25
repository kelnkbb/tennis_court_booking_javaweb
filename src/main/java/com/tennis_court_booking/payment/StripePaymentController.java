package com.tennis_court_booking.payment;

import com.stripe.exception.StripeException;
import com.tennis_court_booking.payment.vo.StripeCheckoutVO;
import com.tennis_court_booking.pojo.vo.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/payments/stripe")
@RequiredArgsConstructor
public class StripePaymentController {

    private final StripePaymentService stripePaymentService;

    /**
     * 创建 Checkout 会话，返回跳转 URL（需登录，且只能为自己的待付款订单发起）。
     */
    @PostMapping("/checkout-session")
    public Result<StripeCheckoutVO> createCheckoutSession(
            @RequestBody(required = false) Map<String, Integer> body,
            @RequestAttribute("userId") Integer userId) {
        Integer bookingId = body == null ? null : body.get("bookingId");
        try {
            StripeCheckoutVO vo = stripePaymentService.createCheckoutSession(bookingId, userId);
            return Result.success(vo);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.error(400, e.getMessage());
        } catch (StripeException e) {
            return Result.error(502, "Stripe 接口异常: " + e.getMessage());
        }
    }

    /**
     * Stripe Webhook（无需 JWT；使用 Stripe-Signature 验签）。
     */
    @PostMapping(value = "/webhook", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> webhook(
            @RequestBody String payload,
            jakarta.servlet.http.HttpServletRequest request) {
        String sig = request.getHeader("Stripe-Signature");
        boolean ok = stripePaymentService.handleWebhookPayload(payload, sig);
        if (!ok) {
            return ResponseEntity.badRequest().body("rejected");
        }
        return ResponseEntity.ok("ok");
    }
}
