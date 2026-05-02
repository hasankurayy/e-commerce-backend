package com.ecommerce.order.entity;

public enum OrderStatus {
    PENDING_PAYMENT,
    PAYMENT_PROCESSING,
    PAYMENT_FAILED,
    PAID,
    PROCESSING,
    SHIPPED,
    DELIVERED,
    REFUND_REQUESTED,
    REFUNDED,
    CANCELLED
}
