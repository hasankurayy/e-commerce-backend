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
public class OrderEventListener {

    private final NotificationService notificationService;

    @RabbitListener(queues = "notification.order.queue")
    public void handleOrderEvent(OrderEvent event) {
        log.info("Received order event: type={}, orderId={}", event.type(), event.orderId());

        if ("order.created".equals(event.type()) || "ORDER_CREATED".equals(event.type())) {
            notificationService.sendEmail(
                    "user-" + event.userId() + "@ecommerce.com",
                    "Siparişiniz Alındı #" + event.orderId(),
                    String.format("Siparişiniz başarıyla oluşturuldu.\nSipariş No: %d\nTutar: %.2f TL",
                            event.orderId(), event.totalAmount()),
                    "ORDER_CREATED", event.orderId(), event.userId()
            );
        } else if ("ORDER_SHIPPED".equals(event.type())) {
            notificationService.sendEmail(
                    "user-" + event.userId() + "@ecommerce.com",
                    "Siparişiniz Kargoya Verildi #" + event.orderId(),
                    String.format("Sipariş No: %d\nTakip No: %s", event.orderId(), event.trackingNumber()),
                    "ORDER_SHIPPED", event.orderId(), event.userId()
            );
        }
    }

    public record OrderEvent(String type, Long orderId, Long userId,
                             BigDecimal totalAmount, String trackingNumber) {}
}
