package com.ecommerce.product.controller;

import com.ecommerce.common.dto.ApiResponse;
import com.ecommerce.product.dto.ReviewRequest;
import com.ecommerce.product.dto.ReviewResponse;
import com.ecommerce.product.dto.ReviewSummaryResponse;
import com.ecommerce.product.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products/{productId}/reviews")
@RequiredArgsConstructor
@Tag(name = "Reviews", description = "Ürün değerlendirme — yalnızca teslim edilmiş siparişlerdeki ürünler değerlendirilebilir")
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary = "Ürün değerlendirmesi ekle",
        description = "Kullanıcının DELIVERED durumunda sipariş ettiği ürüne 1-5 yıldız değerlendirme ekler. " +
                      "Her kullanıcı bir ürünü yalnızca bir kez değerlendirebilir."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Değerlendirme kaydedildi"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bu ürünü daha önce değerlendirdiniz"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Bu ürünü satın almadığınız için değerlendiremezsiniz"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Kimlik doğrulama gerekli")
    })
    public ResponseEntity<ApiResponse<ReviewResponse>> addReview(
            @Parameter(description = "Ürün ID", example = "5") @PathVariable Long productId,
            @Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId,
            @Parameter(hidden = true) @RequestHeader("X-User-Email") String userEmail,
            @Valid @RequestBody ReviewRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Değerlendirme kaydedildi",
                        reviewService.addReview(productId, userId, userEmail, request)));
    }

    @GetMapping("/summary")
    @Operation(
        summary = "Değerlendirme özetini getir",
        description = "Ürünün ortalama puanı, toplam değerlendirme sayısı ve son 5 değerlendiriciyi döner. " +
                      "Kimlik doğrulama opsiyoneldir — giriş yapılmışsa kullanıcının kendi değerlendirmesi de (`myReview`) eklenir."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Değerlendirme özeti")
    })
    public ResponseEntity<ApiResponse<ReviewSummaryResponse>> getSummary(
            @Parameter(description = "Ürün ID", example = "5") @PathVariable Long productId,
            @Parameter(hidden = true) @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return ResponseEntity.ok(ApiResponse.success(reviewService.getSummary(productId, userId)));
    }
}
