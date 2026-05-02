package com.ecommerce.order.messaging;

import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderStatus;
import com.ecommerce.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventListener {

    private final OrderRepository orderRepository;

    @RabbitListener(queues = "order.payment.queue")
    @Transactional
    public void handlePaymentEvent(PaymentEvent event) {
        log.info("Received payment event: type={}, orderId={}", event.type(), event.orderId());

        Order order = orderRepository.findById(event.orderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order", event.orderId()));

        if ("PAYMENT_COMPLETED".equals(event.type())) {
            order.setStatus(OrderStatus.PAID);
        } else if ("PAYMENT_FAILED".equals(event.type())) {
            order.setStatus(OrderStatus.PAYMENT_FAILED);
        }

        orderRepository.save(order);
    }

    public record PaymentEvent(String type, Long orderId, Long userId, java.math.BigDecimal amount) {}
}
