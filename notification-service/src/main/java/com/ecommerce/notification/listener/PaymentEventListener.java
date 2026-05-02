package com.ecommerce.notification.listener;

import com.ecommerce.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventListener {

    private final NotificationService notificationService;

    @RabbitListener(queues = "notification.payment.queue")
    public void handlePaymentEvent(PaymentEvent event) {
        log.info("Received payment event: type={}, orderId={}", event.type(), event.orderId());

        if ("PAYMENT_COMPLETED".equals(event.type())) {
            notificationService.sendEmail(
                    "user-" + event.userId() + "@ecommerce.com",
                    "Ödemeniz Onaylandı #" + event.orderId(),
                    String.format("Sipariş #%d için %.2f TL ödemeniz başarıyla alındı.",
                            event.orderId(), event.amount()),
                    "PAYMENT_COMPLETED", event.orderId(), event.userId()
            );
        } else if ("PAYMENT_FAILED".equals(event.type())) {
            notificationService.sendEmail(
                    "user-" + event.userId() + "@ecommerce.com",
                    "Ödeme Başarısız #" + event.orderId(),
                    String.format("Sipariş #%d için ödemeniz alınamadı. Lütfen tekrar deneyiniz.",
                            event.orderId()),
                    "PAYMENT_FAILED", event.orderId(), event.userId()
            );
        }
    }

    public record PaymentEvent(String type, Long orderId, Long userId, BigDecimal amount) {}
}
