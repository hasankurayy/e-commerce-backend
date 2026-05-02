package com.ecommerce.cart.client;

import com.ecommerce.common.dto.ApiResponse;
import org.springframework.stereotype.Component;

@Component
public class ProductFeignClientFallback implements ProductFeignClient {

    @Override
    public ApiResponse<ProductDto> getProduct(Long id) {
        return ApiResponse.error("Product service unavailable");
    }

    @Override
    public ApiResponse<Void> decreaseStock(Long id, int quantity) {
        return ApiResponse.error("Product service unavailable");
    }
}
