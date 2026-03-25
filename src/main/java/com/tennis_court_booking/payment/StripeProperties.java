package com.tennis_court_booking.payment;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Stripe Checkout：密钥与回调 URL 建议用环境变量注入，勿提交真实密钥。
 */
@ConfigurationProperties(prefix = "stripe")
public class StripeProperties {

    /** Secret Key（sk_test_... / sk_live_...） */
    private String secretKey = "";

    /** Webhook 签名密钥（whsec_...） */
    private String webhookSecret = "";

    /**
     * 支付成功跳转（需与 Stripe Dashboard 中允许的域名一致；可带 query）
     * 建议包含 {CHECKOUT_SESSION_ID} 占位由 Stripe 替换
     */
    private String successUrl = "http://localhost:5173/my-bookings?stripe=success";

    /** 用户取消支付返回 */
    private String cancelUrl = "http://localhost:5173/my-bookings?stripe=cancel";

    /** ISO 货币代码，与订单金额币种一致；预约金额以人民币元存储时用 cny */
    private String currency = "cny";

    public boolean isEnabled() {
        return secretKey != null && !secretKey.isBlank();
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getWebhookSecret() {
        return webhookSecret;
    }

    public void setWebhookSecret(String webhookSecret) {
        this.webhookSecret = webhookSecret;
    }

    public String getSuccessUrl() {
        return successUrl;
    }

    public void setSuccessUrl(String successUrl) {
        this.successUrl = successUrl;
    }

    public String getCancelUrl() {
        return cancelUrl;
    }

    public void setCancelUrl(String cancelUrl) {
        this.cancelUrl = cancelUrl;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
}
