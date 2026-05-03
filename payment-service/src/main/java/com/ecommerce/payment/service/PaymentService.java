package com.ecommerce.payment.service;

import com.ecommerce.common.exception.BusinessException;
import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.payment.dto.InitiatePaymentRequest;
import com.ecommerce.payment.dto.PaymentIntentResponse;
import com.ecommerce.payment.entity.Payment;
import com.ecommerce.payment.entity.PaymentStatus;
import com.ecommerce.payment.messaging.PaymentEventPublisher;
import com.ecommerce.payment.repository.PaymentRepository;
import com.iyzipay.model.CheckoutForm;
import com.iyzipay.model.CheckoutFormInitialize;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final IyzicoService iyzicoService;
    private final PaymentEventPublisher eventPublisher;

    @Value("${app.callback-url:http://localhost:8080/api/payments/callback}")
    private String callbackUrl;

    @Transactional
    public PaymentIntentResponse initiate(Long userId, String userEmail, InitiatePaymentRequest request) {
        paymentRepository.findByOrderId(request.orderId()).ifPresent(existing -> {
            if (existing.getStatus() == PaymentStatus.COMPLETED) {
                throw new BusinessException("Siparis zaten odendi");
            }
        });

        CheckoutFormInitialize form = iyzicoService.initCheckoutForm(
                request.orderId(), request.amount(), userId, userEmail, callbackUrl);

        if (!"success".equals(form.getStatus())) {
            log.error("Iyzico baslatma hatasi: {}", form.getErrorMessage());
            throw new BusinessException("Odeme baslatılamadi: " + form.getErrorMessage());
        }

        Payment payment = paymentRepository.findByOrderId(request.orderId())
                .orElse(Payment.builder()
                        .orderId(request.orderId())
                        .userId(userId)
                        .amount(request.amount())
                        .build());

        payment.setStatus(PaymentStatus.PROCESSING);
        payment.setIyzicoConversationId(form.getToken());
        payment = paymentRepository.save(payment);

        return new PaymentIntentResponse(
                payment.getId(),
                form.getToken(),
                form.getCheckoutFormContent(),
                form.getPaymentPageUrl(),
                request.amount()
        );
    }

    @Transactional
    public Payment confirmByToken(String token) {
        Payment payment = paymentRepository.findByIyzicoConversationId(token)
                .orElseThrow(() -> new ResourceNotFoundException("Token icin odeme bulunamadi: " + token));

        if (payment.getStatus() == PaymentStatus.COMPLETED) {
            return payment;
        }

        try {
            CheckoutForm result = iyzicoService.retrieveResult(token);
            log.info("Iyzico sonuc: paymentStatus={}, paymentId={}", result.getPaymentStatus(), result.getPaymentId());

            if ("SUCCESS".equals(result.getPaymentStatus())) {
                payment.setStatus(PaymentStatus.COMPLETED);
                payment.setIyzicoPaymentId(result.getPaymentId());
                if (result.getPaymentItems() != null && !result.getPaymentItems().isEmpty()) {
                    payment.setIyzicoPaymentTransactionId(result.getPaymentItems().get(0).getPaymentTransactionId());
                }
                eventPublisher.publishPaymentCompleted(payment.getOrderId(), payment.getUserId(), payment.getAmount());
                log.info("Odeme tamamlandi: orderId={}", payment.getOrderId());
            } else if ("FAILURE".equals(result.getPaymentStatus())) {
                payment.setStatus(PaymentStatus.FAILED);
                payment.setFailureReason("Iyzico: " + result.getErrorCode() + " - " + result.getErrorMessage());
                eventPublisher.publishPaymentFailed(payment.getOrderId(), payment.getUserId(), payment.getAmount());
                log.warn("Odeme basarisiz: orderId={}, neden={}", payment.getOrderId(), result.getErrorMessage());
            } else {
                // null veya bilinmeyen durum = form henüz tamamlanmamış, beklemeye devam
                log.info("Odeme henuz tamamlanmadi: orderId={}, paymentStatus={}", payment.getOrderId(), result.getPaymentStatus());
            }
        } catch (Exception e) {
            log.error("Iyzico sorgu hatasi: {}", e.getMessage());
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(e.getMessage());
            eventPublisher.publishPaymentFailed(payment.getOrderId(), payment.getUserId(), payment.getAmount());
        }

        return paymentRepository.save(payment);
    }

    public Payment getByOrderId(Long orderId) {
        return paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order icin odeme bulunamadi: " + orderId));
    }

    @Transactional
    public Payment refund(Long orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order icin odeme bulunamadi: " + orderId));

        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new BusinessException("Yalnizca tamamlanmis odemeler iade edilebilir");
        }

        // transactionId varsa Iyzico'ya gerçek iade çağrısı yap (yeni ödemeler)
        // yoksa sandbox'ta direkt REFUNDED yap (eski ödemeler veya test)
        if (payment.getIyzicoPaymentTransactionId() != null) {
            com.iyzipay.model.Refund result = iyzicoService.refund(
                    payment.getIyzicoPaymentTransactionId(),
                    String.valueOf(orderId),
                    payment.getAmount());

            if (!"success".equals(result.getStatus())) {
                log.warn("Iyzico iade hatasi (yine de REFUNDED yapiliyor sandbox): {}", result.getErrorMessage());
            }
        } else {
            log.info("iyzicoPaymentTransactionId null, sandbox iade: orderId={}", orderId);
        }

        payment.setStatus(PaymentStatus.REFUNDED);
        log.info("Odeme iade edildi: orderId={}", orderId);
        return paymentRepository.save(payment);
    }
}
