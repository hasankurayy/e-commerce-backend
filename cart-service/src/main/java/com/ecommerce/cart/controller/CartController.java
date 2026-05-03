package com.ecommerce.cart.controller;

import com.ecommerce.cart.dto.AddItemRequest;
import com.ecommerce.cart.dto.CartResponse;
import com.ecommerce.cart.service.CartService;
import com.ecommerce.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Tag(name = "Cart", description = "Alışveriş sepeti — ürün ekleme, miktar güncelleme, çıkarma ve temizleme")
@SecurityRequirement(name = "bearerAuth")
public class CartController {

    private final CartService cartService;

    @GetMapping
    @Operation(summary = "Sepeti görüntüle", description = "Giriş yapan kullanıcının aktif sepetini tüm ürünler ve toplam tutar ile getirir.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Sepet içeriği"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Kimlik doğrulama gerekli")
    })
    public ResponseEntity<ApiResponse<CartResponse>> getCart(
            @Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(ApiResponse.success(cartService.getCart(userId)));
    }

    @PostMapping("/items")
    @Operation(summary = "Sepete ürün ekle", description = "Belirtilen ürünü sepete ekler. Ürün zaten sepette varsa miktarı artırır. Stok kontrolü yapılır.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Ürün eklendi, güncel sepet döner"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Geçersiz miktar veya yetersiz stok"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Ürün bulunamadı")
    })
    public ResponseEntity<ApiResponse<CartResponse>> addItem(
            @Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody AddItemRequest request) {
        return ResponseEntity.ok(ApiResponse.success(cartService.addItem(userId, request)));
    }

    @PutMapping("/items/{productId}")
    @Operation(summary = "Sepet ürün miktarını güncelle", description = "Sepetteki bir ürünün miktarını günceller. Miktar 0 gönderilirse ürün sepetten çıkarılır.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Miktar güncellendi, güncel sepet döner"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Yetersiz stok"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Ürün sepette bulunamadı")
    })
    public ResponseEntity<ApiResponse<CartResponse>> updateItem(
            @Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId,
            @Parameter(description = "Ürün ID", example = "3") @PathVariable Long productId,
            @Parameter(description = "Yeni miktar (0 = sepetten çıkar)", example = "2") @RequestParam int quantity) {
        return ResponseEntity.ok(ApiResponse.success(cartService.updateItem(userId, productId, quantity)));
    }

    @DeleteMapping("/items/{productId}")
    @Operation(summary = "Sepetten ürün çıkar", description = "Belirtilen ürünü miktarından bağımsız olarak sepetten tamamen kaldırır.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Ürün çıkarıldı, güncel sepet döner"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Ürün sepette bulunamadı")
    })
    public ResponseEntity<ApiResponse<CartResponse>> removeItem(
            @Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId,
            @Parameter(description = "Ürün ID", example = "3") @PathVariable Long productId) {
        return ResponseEntity.ok(ApiResponse.success(cartService.removeItem(userId, productId)));
    }

    @DeleteMapping
    @Operation(summary = "Sepeti temizle", description = "Kullanıcının sepetindeki tüm ürünleri siler. Sipariş oluşturulduktan sonra otomatik olarak da çağrılır.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Sepet temizlendi"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Kimlik doğrulama gerekli")
    })
    public ResponseEntity<ApiResponse<Void>> clearCart(
            @Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId) {
        cartService.clearCart(userId);
        return ResponseEntity.ok(ApiResponse.success("Cart cleared"));
    }
}
