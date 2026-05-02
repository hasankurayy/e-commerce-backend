package com.ecommerce.order.client;

import com.ecommerce.common.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import java.math.BigDecimal;
import java.util.List;

@FeignClient(name = "cart-service")
public interface CartFeignClient {

    @GetMapping("/api/cart")
    ApiResponse<CartDto> getCart(@RequestHeader("X-User-Id") Long userId);

    @DeleteMapping("/api/cart")
    ApiResponse<Void> clearCart(@RequestHeader("X-User-Id") Long userId);

    record CartDto(
            Long cartId, Long userId,
            List<CartItemDto> items,
            BigDecimal totalAmount, int totalItems) {}

    record CartItemDto(
            Long itemId, Long productId, String productName,
            BigDecimal unitPrice, Integer quantity, BigDecimal subtotal) {}
}
