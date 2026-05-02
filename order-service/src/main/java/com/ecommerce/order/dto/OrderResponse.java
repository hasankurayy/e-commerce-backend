package com.ecommerce.order.dto;

import com.ecommerce.order.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
        Long id,
        Long userId,
        OrderStatus status,
        BigDecimal totalAmount,
        String shippingAddress,
        String trackingNumber,
        List<OrderItemResponse> items,
        Instant createdAt
) {
    public record OrderItemResponse(
            Long productId, String productName,
            BigDecimal unitPrice, Integer quantity, BigDecimal subtotal
    ) {}
}
