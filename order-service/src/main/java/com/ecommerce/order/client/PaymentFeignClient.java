package com.ecommerce.order.client;

import com.ecommerce.common.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = "payment-service")
public interface PaymentFeignClient {

    @PostMapping("/api/payments/order/{orderId}/refund")
    ApiResponse<Void> refund(@PathVariable Long orderId);
}
