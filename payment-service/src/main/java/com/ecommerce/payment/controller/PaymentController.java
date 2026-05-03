package com.ecommerce.payment.controller;

import com.ecommerce.common.dto.ApiResponse;
import com.ecommerce.payment.dto.InitiatePaymentRequest;
import com.ecommerce.payment.dto.PaymentIntentResponse;
import com.ecommerce.payment.entity.Payment;
import com.ecommerce.payment.entity.PaymentStatus;
import com.ecommerce.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Payments", description = "Iyzico Checkout Form ile 3D Secure ödeme işleme")
public class PaymentController {

    private final PaymentService paymentService;

    @Value("${app.frontend-url:http://localhost:3001}")
    private String frontendUrl;

    @PostMapping("/initiate")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary = "Ödemeyi başlat",
        description = "Iyzico Checkout Form başlatır ve ödeme sayfasına yönlendirmek için gereken token ile HTML içeriğini döner. " +
                      "Frontend bu token ile Iyzico'nun iframe/form'unu render eder."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Iyzico form token ve HTML içeriği"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Geçersiz sipariş veya ödeme zaten tamamlanmış"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Kimlik doğrulama gerekli")
    })
    public ResponseEntity<ApiResponse<PaymentIntentResponse>> initiate(
            @Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId,
            @Parameter(hidden = true) @RequestHeader(value = "X-User-Email", defaultValue = "user@example.com") String userEmail,
            @Valid @RequestBody InitiatePaymentRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                paymentService.initiate(userId, userEmail, request)));
    }

    @PostMapping("/callback")
    @Operation(
        summary = "Iyzico ödeme callback",
        description = "Iyzico'nun ödeme sonrasında tarayıcıyı yönlendirdiği callback endpoint'i. " +
                      "Ödeme başarılıysa frontend'e `/orders?payment=success`, başarısızsa `/orders?payment=failed` yönlendirir. " +
                      "**Bu endpoint doğrudan çağrılmamalıdır — yalnızca Iyzico tarafından kullanılır.**"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "302", description = "Frontend'e yönlendirme")
    })
    public void callback(
            @Parameter(description = "Iyzico ödeme token'ı") @RequestParam String token,
            HttpServletResponse response) throws IOException {
        log.info("Iyzico callback geldi: token={}", token);
        try {
            Payment payment = paymentService.confirmByToken(token);
            if (payment.getStatus() == PaymentStatus.COMPLETED) {
                response.sendRedirect(frontendUrl + "/orders?payment=success");
            } else {
                response.sendRedirect(frontendUrl + "/orders?payment=failed");
            }
        } catch (Exception e) {
            log.error("Callback hatasi: {}", e.getMessage());
            response.sendRedirect(frontendUrl + "/orders?payment=error");
        }
    }

    @GetMapping("/result/{token}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary = "Ödeme sonucunu sorgula",
        description = "Frontend'in ödeme tamamlandıktan sonra durumu kontrol etmek için kullandığı polling endpoint'i."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Ödeme bilgisi"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Token ile eşleşen ödeme bulunamadı")
    })
    public ResponseEntity<ApiResponse<Payment>> getResult(
            @Parameter(description = "Iyzico ödeme token'ı") @PathVariable String token) {
        Payment payment = paymentService.confirmByToken(token);
        return ResponseEntity.ok(ApiResponse.success(payment));
    }

    @PostMapping("/order/{orderId}/refund")
    @Operation(
        summary = "Ödeme iadesi yap (dahili)",
        description = "Siparişin ödemesini Iyzico üzerinden iade eder. order-service tarafından çağrılır."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "İade başarılı"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "İade yapılamaz")
    })
    public ResponseEntity<ApiResponse<Payment>> refund(
            @Parameter(description = "Sipariş ID", example = "1") @PathVariable Long orderId) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.refund(orderId)));
    }

    @GetMapping("/order/{orderId}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Siparişe ait ödemeyi getir", description = "Belirtilen siparişe ait ödeme kaydını döner.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Ödeme kaydı"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Sipariş veya ödeme bulunamadı")
    })
    public ResponseEntity<ApiResponse<Payment>> getByOrder(
            @Parameter(description = "Sipariş ID", example = "1") @PathVariable Long orderId) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.getByOrderId(orderId)));
    }
}
