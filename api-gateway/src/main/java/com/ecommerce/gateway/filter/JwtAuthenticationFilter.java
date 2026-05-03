package com.ecommerce.gateway.filter;

import com.ecommerce.common.constants.SecurityConstants;
import com.ecommerce.common.security.JwtUtil;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/refresh",
            "/api/products",
            "/api/categories",
            "/api/payments/callback",
            // Swagger UI — per-service routes (gateway strips the prefix)
            "/user-service/swagger-ui", "/user-service/v3/api-docs", "/user-service/webjars",
            "/product-service/swagger-ui", "/product-service/v3/api-docs", "/product-service/webjars",
            "/cart-service/swagger-ui", "/cart-service/v3/api-docs", "/cart-service/webjars",
            "/order-service/swagger-ui", "/order-service/v3/api-docs", "/order-service/webjars",
            "/payment-service/swagger-ui", "/payment-service/v3/api-docs", "/payment-service/webjars"
    );

    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        String authHeader = exchange.getRequest()
                .getHeaders()
                .getFirst(SecurityConstants.AUTHORIZATION_HEADER);

        boolean hasValidToken = false;
        ServerWebExchange processedExchange = exchange;

        // JWT varsa ve geçerliyse her zaman header'ları enjekte et (public path olsa bile)
        if (authHeader != null && authHeader.startsWith(SecurityConstants.BEARER_PREFIX)) {
            String token = authHeader.substring(SecurityConstants.BEARER_PREFIX.length());
            if (jwtUtil.validateToken(token)) {
                hasValidToken = true;
                String userId = String.valueOf(jwtUtil.extractUserId(token));
                String email = jwtUtil.extractEmail(token);
                List<String> roles = jwtUtil.extractRoles(token);
                processedExchange = exchange.mutate()
                        .request(r -> r.headers(headers -> {
                            headers.set(SecurityConstants.USER_ID_HEADER, userId);
                            headers.set(SecurityConstants.USER_EMAIL_HEADER, email);
                            headers.set(SecurityConstants.USER_ROLES_HEADER, String.join(",", roles));
                        }))
                        .build();
            }
        }

        // Public olmayan path'ler için geçerli token zorunlu
        if (!isPublicPath(path) && !hasValidToken) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        return chain.filter(processedExchange);
    }

    @Override
    public int getOrder() {
        return -1;
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }
}
