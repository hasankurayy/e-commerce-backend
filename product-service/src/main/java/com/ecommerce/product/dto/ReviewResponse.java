package com.ecommerce.product.dto;

import java.time.Instant;

public record ReviewResponse(
        Long id,
        Long userId,
        String userEmail,
        String userName,
        int rating,
        String comment,
        Instant createdAt
) {}
