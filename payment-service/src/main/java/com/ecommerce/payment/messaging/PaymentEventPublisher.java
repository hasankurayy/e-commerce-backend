package com.ecommerce.payment.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventPublisher {

    private static final String EXCHANGE = "ecommerce.events";

    private final RabbitTemplate rabbitTemplate;

    public void publishPaymentCompleted(Long orderId, Long userId, BigDecimal amount) {
        PaymentEvent event = new PaymentEvent("PAYMENT_COMPLETED", orderId, userId, amount);
        log.info("Publishing PAYMENT_COMPLETED: orderId={}", orderId);
        rabbitTemplate.convertAndSend(EXCHANGE, "payment.completed", event);
    }

    public void publishPaymentFailed(Long orderId, Long userId, BigDecimal amount) {
        PaymentEvent event = new PaymentEvent("PAYMENT_FAILED", orderId, userId, amount);
        log.info("Publishing PAYMENT_FAILED: orderId={}", orderId);
        rabbitTemplate.convertAndSend(EXCHANGE, "payment.failed", event);
    }

    public record PaymentEvent(String type, Long orderId, Long userId, BigDecimal amount) {}
}
