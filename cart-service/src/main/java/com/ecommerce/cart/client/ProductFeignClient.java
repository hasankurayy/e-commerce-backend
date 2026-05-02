package com.ecommerce.cart.client;

import com.ecommerce.common.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "product-service", fallback = ProductFeignClientFallback.class)
public interface ProductFeignClient {

    @GetMapping("/api/products/{id}")
    ApiResponse<ProductDto> getProduct(@PathVariable Long id);

    @PatchMapping("/api/products/{id}/stock/decrease")
    ApiResponse<Void> decreaseStock(@PathVariable Long id, @RequestParam int quantity);

    record ProductDto(Long id, String name, java.math.BigDecimal price, Integer stock, boolean active) {}
}
