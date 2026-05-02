package com.ecommerce.common.security;

import java.util.List;

public record UserPrincipal(
        Long id,
        String email,
        List<String> roles
) {}
