package com.ecommerce.product.client;

import com.ecommerce.common.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "order-service")
public interface OrderFeignClient {

    @GetMapping("/api/orders/has-ordered/{productId}")
    ApiResponse<Boolean> hasOrdered(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long productId);
}
