package com.ecommerce.order.service;

import com.ecommerce.common.dto.PageResponse;
import com.ecommerce.common.exception.BusinessException;
import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.order.client.CartFeignClient;
import com.ecommerce.order.dto.CreateOrderRequest;
import com.ecommerce.order.dto.OrderResponse;
import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderItem;
import com.ecommerce.order.entity.OrderStatus;
import com.ecommerce.order.messaging.OrderEventPublisher;
import com.ecommerce.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class OrderService {

    private static final Set<OrderStatus> HIDDEN_STATUSES = Set.of(
            OrderStatus.PENDING_PAYMENT, OrderStatus.PAYMENT_FAILED
    );

    private final OrderRepository orderRepository;
    private final CartFeignClient cartFeignClient;
    private final OrderEventPublisher eventPublisher;

    @Transactional
    public OrderResponse createOrder(Long userId, CreateOrderRequest request) {
        var cartResponse = cartFeignClient.getCart(userId);
        if (!cartResponse.success() || cartResponse.data() == null) {
            throw new BusinessException("Could not retrieve cart");
        }

        var cart = cartResponse.data();
        if (cart.items().isEmpty()) {
            throw new BusinessException("Cart is empty");
        }

        List<OrderItem> items = cart.items().stream()
                .map(i -> OrderItem.builder()
                        .productId(i.productId())
                        .productName(i.productName())
                        .unitPrice(i.unitPrice())
                        .quantity(i.quantity())
                        .build())
                .toList();

        Order order = Order.builder()
                .userId(userId)
                .status(OrderStatus.PENDING_PAYMENT)
                .totalAmount(cart.totalAmount())
                .shippingAddress(request.shippingAddress())
                .build();

        order = orderRepository.save(order);

        final Order savedOrder = order;
        items.forEach(item -> {
            item.setOrder(savedOrder);
            savedOrder.getItems().add(item);
        });

        orderRepository.save(savedOrder);

        // Sepeti temizle
        cartFeignClient.clearCart(userId);

        // Event yayınla
        eventPublisher.publishOrderCreated(savedOrder.getId(), userId, cart.totalAmount());

        return toResponse(savedOrder);
    }

    public PageResponse<OrderResponse> getUserOrders(Long userId, int page, int size) {
        Page<Order> orders = orderRepository.findByUserIdAndStatusNotInOrderByCreatedAtDesc(
                userId, HIDDEN_STATUSES, PageRequest.of(page, size));
        var content = orders.getContent().stream().map(this::toResponse).toList();
        return PageResponse.of(content, orders.getNumber(), orders.getSize(), orders.getTotalElements());
    }

    public OrderResponse getById(Long id, Long userId) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", id));
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException("Access denied");
        }
        return toResponse(order);
    }

    @Transactional
    public OrderResponse cancel(Long id, Long userId) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", id));

        if (!order.getUserId().equals(userId)) {
            throw new BusinessException("Access denied");
        }

        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new BusinessException("Only PENDING_PAYMENT orders can be cancelled");
        }

        order.setStatus(OrderStatus.CANCELLED);
        return toResponse(orderRepository.save(order));
    }

    @Transactional
    public OrderResponse ship(Long id, String trackingNumber) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", id));

        if (order.getStatus() != OrderStatus.PAID) {
            throw new BusinessException("Only PAID orders can be shipped");
        }

        order.setStatus(OrderStatus.SHIPPED);
        order.setTrackingNumber(trackingNumber);
        order = orderRepository.save(order);
        eventPublisher.publishOrderShipped(order.getId(), order.getUserId(), trackingNumber);
        return toResponse(order);
    }

    public boolean hasOrderedProduct(Long userId, Long productId) {
        return orderRepository.existsByUserIdAndStatusAndItemsProductId(userId, OrderStatus.DELIVERED, productId);
    }

    private OrderResponse toResponse(Order order) {
        List<OrderResponse.OrderItemResponse> items = order.getItems().stream()
                .map(i -> new OrderResponse.OrderItemResponse(
                        i.getProductId(), i.getProductName(),
                        i.getUnitPrice(), i.getQuantity(), i.getSubtotal()))
                .toList();

        return new OrderResponse(order.getId(), order.getUserId(), order.getStatus(),
                order.getTotalAmount(), order.getShippingAddress(),
                order.getTrackingNumber(), items, order.getCreatedAt());
    }
}
