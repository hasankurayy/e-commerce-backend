package com.ecommerce.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class GatewayConfig {

    private static final String DEDUP_CORS =
            "Access-Control-Allow-Credentials Access-Control-Allow-Origin Access-Control-Allow-Methods Access-Control-Allow-Headers";

    @Bean
    public RouteLocator routeLocator(RouteLocatorBuilder builder) {
        return builder.routes()

                .route("user-auth", r -> r
                        .path("/api/auth/**")
                        .filters(f -> f.dedupeResponseHeader(DEDUP_CORS, "RETAIN_FIRST"))
                        .uri("lb://user-service"))

                .route("user-profile", r -> r
                        .path("/api/users/**")
                        .filters(f -> f.dedupeResponseHeader(DEDUP_CORS, "RETAIN_FIRST"))
                        .uri("lb://user-service"))

                .route("product-service", r -> r
                        .path("/api/products/**")
                        .filters(f -> f.dedupeResponseHeader(DEDUP_CORS, "RETAIN_FIRST"))
                        .uri("lb://product-service"))

                .route("category-service", r -> r
                        .path("/api/categories/**")
                        .filters(f -> f.dedupeResponseHeader(DEDUP_CORS, "RETAIN_FIRST"))
                        .uri("lb://product-service"))

                .route("cart-service", r -> r
                        .path("/api/cart/**")
                        .filters(f -> f.dedupeResponseHeader(DEDUP_CORS, "RETAIN_FIRST"))
                        .uri("lb://cart-service"))

                .route("order-service", r -> r
                        .path("/api/orders/**")
                        .filters(f -> f.dedupeResponseHeader(DEDUP_CORS, "RETAIN_FIRST"))
                        .uri("lb://order-service"))

                .route("payment-service", r -> r
                        .path("/api/payments/**")
                        .filters(f -> f.dedupeResponseHeader(DEDUP_CORS, "RETAIN_FIRST"))
                        .uri("lb://payment-service"))

                .build();
    }

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsWebFilter(source);
    }
}
