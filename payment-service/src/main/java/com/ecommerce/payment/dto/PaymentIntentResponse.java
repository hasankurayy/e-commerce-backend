package com.ecommerce.payment.dto;

import java.math.BigDecimal;

public record PaymentIntentResponse(
        Long paymentId,
        String token,
        String checkoutFormContent,
        String paymentPageUrl,
        BigDecimal amount
) {}
