package com.ecommerce.payment.dto;

import jakarta.validation.constraints.NotNull;

public record InitiatePaymentRequest(
        @NotNull Long orderId,
        @NotNull java.math.BigDecimal amount
) {}
