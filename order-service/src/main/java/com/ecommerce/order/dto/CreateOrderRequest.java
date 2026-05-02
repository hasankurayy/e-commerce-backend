package com.ecommerce.order.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateOrderRequest(@NotBlank String shippingAddress) {}
