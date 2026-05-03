package com.ecommerce.product.controller;

import com.ecommerce.common.dto.ApiResponse;
import com.ecommerce.common.dto.PageResponse;
import com.ecommerce.product.dto.ProductRequest;
import com.ecommerce.product.dto.ProductResponse;
import com.ecommerce.product.service.ProductService;
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
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Ürün kataloğu — listeleme, arama, kategori filtresi ve yönetim")
public class ProductController {

    private final ProductService productService;

    @GetMapping
    @Operation(summary = "Aktif ürünleri listele", description = "Tüm aktif ürünleri sayfalı ve sıralı olarak döner. Kimlik doğrulama gerektirmez.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Ürün listesi")
    })
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> getAll(
            @Parameter(description = "Sayfa numarası (0'dan başlar)", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Sayfa başına ürün sayısı", example = "20") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sıralama alanı", example = "createdAt") @RequestParam(defaultValue = "createdAt") String sortBy) {
        return ResponseEntity.ok(ApiResponse.success(productService.getAll(page, size, sortBy)));
    }

    @GetMapping("/search")
    @Operation(summary = "Ürün ara", description = "Ürün adına göre büyük/küçük harf duyarsız arama yapar.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Arama sonuçları")
    })
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> search(
            @Parameter(description = "Arama terimi", example = "laptop") @RequestParam String q,
            @Parameter(description = "Sayfa numarası", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Sayfa büyüklüğü", example = "20") @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(productService.search(q, page, size)));
    }

    @GetMapping("/category/{categoryId}")
    @Operation(summary = "Kategoriye göre ürünleri listele", description = "Belirtilen kategorideki aktif ürünleri sayfalı olarak döner.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Kategori ürün listesi"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Kategori bulunamadı")
    })
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> getByCategory(
            @Parameter(description = "Kategori ID", example = "1") @PathVariable Long categoryId,
            @Parameter(description = "Sayfa numarası", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Sayfa büyüklüğü", example = "20") @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(productService.getByCategory(categoryId, page, size)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Ürün detayını getir", description = "Belirtilen ID'ye sahip ürünün tüm bilgilerini döner.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Ürün detayı"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Ürün bulunamadı")
    })
    public ResponseEntity<ApiResponse<ProductResponse>> getById(
            @Parameter(description = "Ürün ID", example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(productService.getById(id)));
    }

    @PostMapping
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Yeni ürün oluştur", description = "Yeni bir ürün kaydı oluşturur. Admin yetkisi gerektirir.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Ürün oluşturuldu"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Geçersiz istek"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Kimlik doğrulama gerekli")
    })
    public ResponseEntity<ApiResponse<ProductResponse>> create(@Valid @RequestBody ProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Product created", productService.create(request)));
    }

    @PutMapping("/{id}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Ürün güncelle", description = "Belirtilen ürünün bilgilerini günceller. Admin yetkisi gerektirir.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Ürün güncellendi"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Ürün bulunamadı")
    })
    public ResponseEntity<ApiResponse<ProductResponse>> update(
            @Parameter(description = "Ürün ID", example = "1") @PathVariable Long id,
            @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Product updated", productService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Ürünü pasife al", description = "Ürünü fiziksel olarak silmez, `active=false` yaparak listelerden gizler.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Ürün pasife alındı"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Ürün bulunamadı")
    })
    public ResponseEntity<ApiResponse<Void>> delete(
            @Parameter(description = "Ürün ID", example = "1") @PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Product deleted"));
    }

    @PatchMapping("/{id}/stock/decrease")
    @Operation(summary = "Stok azalt (dahili)", description = "Sipariş oluşturulduğunda order-service tarafından çağrılır. Doğrudan kullanılmamalıdır.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Stok güncellendi"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Yetersiz stok"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Ürün bulunamadı")
    })
    public ResponseEntity<ApiResponse<Void>> decreaseStock(
            @Parameter(description = "Ürün ID", example = "1") @PathVariable Long id,
            @Parameter(description = "Azaltılacak miktar", example = "2") @RequestParam int quantity) {
        productService.decreaseStock(id, quantity);
        return ResponseEntity.ok(ApiResponse.success("Stock updated"));
    }
}
