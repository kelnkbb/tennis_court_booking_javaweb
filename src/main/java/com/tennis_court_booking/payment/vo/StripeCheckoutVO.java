package com.tennis_court_booking.payment.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StripeCheckoutVO {
    private String url;
    private String sessionId;
}
