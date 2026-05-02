package com.ecommerce.order.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventPublisher {

    private static final String EXCHANGE = "ecommerce.events";

    private final RabbitTemplate rabbitTemplate;

    public void publishOrderCreated(Long orderId, Long userId, BigDecimal totalAmount) {
        OrderCreatedEvent event = new OrderCreatedEvent(orderId, userId, totalAmount);
        log.info("Publishing ORDER_CREATED event: orderId={}", orderId);
        rabbitTemplate.convertAndSend(EXCHANGE, "order.created", event);
    }

    public void publishOrderShipped(Long orderId, Long userId, String trackingNumber) {
        OrderShippedEvent event = new OrderShippedEvent(orderId, userId, trackingNumber);
        log.info("Publishing ORDER_SHIPPED event: orderId={}", orderId);
        rabbitTemplate.convertAndSend(EXCHANGE, "order.shipped", event);
    }

    public record OrderCreatedEvent(Long orderId, Long userId, BigDecimal totalAmount) {}
    public record OrderShippedEvent(Long orderId, Long userId, String trackingNumber) {}
}
