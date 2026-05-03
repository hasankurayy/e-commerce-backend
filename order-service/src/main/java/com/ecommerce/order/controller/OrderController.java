package com.ecommerce.order.controller;

import com.ecommerce.common.dto.ApiResponse;
import com.ecommerce.common.dto.PageResponse;
import com.ecommerce.order.dto.CreateOrderRequest;
import com.ecommerce.order.dto.OrderResponse;
import com.ecommerce.order.service.OrderService;
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
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Sipariş oluşturma, listeleme, iptal ve durum yönetimi")
@SecurityRequirement(name = "bearerAuth")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @Operation(
        summary = "Sipariş oluştur",
        description = "Kullanıcının mevcut sepetinden yeni bir sipariş oluşturur ve ödeme sürecini başlatır. Sepet otomatik temizlenir."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Sipariş başarıyla oluşturuldu"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Sepet boş veya geçersiz istek"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Kimlik doğrulama gerekli")
    })
    public ResponseEntity<ApiResponse<OrderResponse>> create(
            @Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody CreateOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Order created", orderService.createOrder(userId, request)));
    }

    @GetMapping
    @Operation(
        summary = "Siparişlerimi listele",
        description = "Giriş yapan kullanıcının tüm siparişlerini sayfalı olarak getirir. PENDING_PAYMENT ve PAYMENT_FAILED durumundaki siparişler dahil edilmez."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Sipariş listesi"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Kimlik doğrulama gerekli")
    })
    public ResponseEntity<ApiResponse<PageResponse<OrderResponse>>> getMyOrders(
            @Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId,
            @Parameter(description = "Sayfa numarası (0'dan başlar)", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Sayfa başına sipariş sayısı", example = "10") @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(orderService.getUserOrders(userId, page, size)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Sipariş detayını getir", description = "Belirtilen ID'ye sahip siparişin detaylarını getirir. Yalnızca siparişin sahibi erişebilir.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Sipariş detayı"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Bu siparişe erişim yetkiniz yok"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Sipariş bulunamadı")
    })
    public ResponseEntity<ApiResponse<OrderResponse>> getById(
            @Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId,
            @Parameter(description = "Sipariş ID", example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(orderService.getById(id, userId)));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Siparişi iptal et", description = "Yalnızca PENDING_PAYMENT durumundaki siparişler iptal edilebilir.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Sipariş iptal edildi"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Sipariş iptal edilemez durumda"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Sipariş bulunamadı")
    })
    public ResponseEntity<ApiResponse<OrderResponse>> cancel(
            @Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId,
            @Parameter(description = "Sipariş ID", example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(orderService.cancel(id, userId)));
    }

    @GetMapping("/has-ordered/{productId}")
    @Operation(summary = "Ürün satın alındı mı kontrol et", description = "Kullanıcının belirtilen ürünü DELIVERED durumunda bir siparişte satın alıp almadığını kontrol eder. Değerlendirme yetkisi doğrulaması için kullanılır.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "true: satın alınmış, false: satın alınmamış")
    })
    public ResponseEntity<ApiResponse<Boolean>> hasOrdered(
            @Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId,
            @Parameter(description = "Ürün ID", example = "5") @PathVariable Long productId) {
        return ResponseEntity.ok(ApiResponse.success(orderService.hasOrderedProduct(userId, productId)));
    }

    @PostMapping("/{id}/return")
    @Operation(summary = "İade talebi oluştur", description = "Yalnızca DELIVERED durumundaki siparişler iade edilebilir. Iyzico refund çağrısı yapılır ve stok geri yüklenir.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "İade başarılı"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "İade yapılamaz durumda"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Sipariş bulunamadı")
    })
    public ResponseEntity<ApiResponse<OrderResponse>> requestReturn(
            @Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId,
            @Parameter(description = "Sipariş ID", example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(orderService.requestReturn(id, userId)));
    }

    @PatchMapping("/{id}/ship")
    @Operation(summary = "Siparişi kargoya ver", description = "Siparişin durumunu SHIPPED olarak günceller ve takip numarası atar. Yalnızca PAID durumundaki siparişler kargoya verilebilir.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Sipariş kargoya verildi"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Sipariş PAID durumunda değil"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Sipariş bulunamadı")
    })
    public ResponseEntity<ApiResponse<OrderResponse>> ship(
            @Parameter(description = "Sipariş ID", example = "1") @PathVariable Long id,
            @Parameter(description = "Kargo takip numarası", example = "TRK-123456") @RequestParam String trackingNumber) {
        return ResponseEntity.ok(ApiResponse.success(orderService.ship(id, trackingNumber)));
    }
}
