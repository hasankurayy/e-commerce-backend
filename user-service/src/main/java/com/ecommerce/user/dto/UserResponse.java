package com.ecommerce.user.dto;

import java.time.Instant;
import java.util.Set;

public record UserResponse(
        Long id,
        String email,
        String firstName,
        String lastName,
        String phone,
        Set<String> roles,
        Instant createdAt
) {}
