package com.ecommerce.order.client;

import com.ecommerce.common.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "product-service")
public interface ProductFeignClient {

    @PatchMapping("/api/products/{id}/stock/increase")
    ApiResponse<Void> increaseStock(@PathVariable Long id, @RequestParam int quantity);
}
