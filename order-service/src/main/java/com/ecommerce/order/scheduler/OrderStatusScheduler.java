package com.ecommerce.order.scheduler;

import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderStatus;
import com.ecommerce.order.messaging.OrderEventPublisher;
import com.ecommerce.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderStatusScheduler {

    private final OrderRepository orderRepository;
    private final OrderEventPublisher eventPublisher;

    private static final Map<OrderStatus, OrderStatus> TRANSITIONS = Map.of(
            OrderStatus.PAID,       OrderStatus.PROCESSING,
            OrderStatus.PROCESSING, OrderStatus.SHIPPED,
            OrderStatus.SHIPPED,    OrderStatus.DELIVERED
    );

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void advanceOrderStatuses() {
        Instant oneMinuteAgo = Instant.now().minus(1, ChronoUnit.MINUTES);

        TRANSITIONS.forEach((from, to) -> {
            List<Order> orders = orderRepository.findByStatusAndUpdatedAtBefore(from, oneMinuteAgo);
            orders.forEach(order -> {
                order.setStatus(to);
                orderRepository.save(order);
                log.info("Order {} advanced: {} -> {}", order.getId(), from, to);

                if (to == OrderStatus.SHIPPED) {
                    eventPublisher.publishOrderShipped(order.getId(), order.getUserId(), "AUTO-" + order.getId());
                }
            });
        });
    }
}
