package com.ecommerce.payment.controller;

import com.ecommerce.common.dto.ApiResponse;
import com.ecommerce.payment.dto.InitiatePaymentRequest;
import com.ecommerce.payment.dto.PaymentIntentResponse;
import com.ecommerce.payment.entity.Payment;
import com.ecommerce.payment.entity.PaymentStatus;
import com.ecommerce.payment.service.PaymentService;
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
@Tag(name = "Payments", description = "Iyzico ile odeme isleme")
public class PaymentController {

    private final PaymentService paymentService;

    @Value("${app.frontend-url:http://localhost:3001}")
    private String frontendUrl;

    // 1. Adım: Iyzico checkout form başlat
    @PostMapping("/initiate")
    public ResponseEntity<ApiResponse<PaymentIntentResponse>> initiate(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-User-Email", defaultValue = "user@example.com") String userEmail,
            @Valid @RequestBody InitiatePaymentRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                paymentService.initiate(userId, userEmail, request)));
    }

    // 2. Adım: Iyzico ödeme sonrası tarayıcıyı buraya yönlendirir
    @PostMapping("/callback")
    public void callback(@RequestParam String token, HttpServletResponse response) throws IOException {
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

    // Frontend polling — kullanıcı "Ödemeyi Kontrol Et" e bastığında
    @GetMapping("/result/{token}")
    public ResponseEntity<ApiResponse<Payment>> getResult(@PathVariable String token) {
        Payment payment = paymentService.confirmByToken(token);
        return ResponseEntity.ok(ApiResponse.success(payment));
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<ApiResponse<Payment>> getByOrder(@PathVariable Long orderId) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.getByOrderId(orderId)));
    }
}
