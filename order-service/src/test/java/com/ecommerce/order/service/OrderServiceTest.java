package com.ecommerce.order.service;

import com.ecommerce.common.dto.ApiResponse;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private CartFeignClient cartFeignClient;
    @Mock private OrderEventPublisher eventPublisher;

    @InjectMocks
    private OrderService orderService;

    private CartFeignClient.CartDto cartWithItems;
    private CartFeignClient.CartDto emptyCart;
    private Order savedOrder;

    @BeforeEach
    void setUp() {
        var item = new CartFeignClient.CartItemDto(
                1L, 100L, "Laptop", new BigDecimal("999.99"), 2, new BigDecimal("1999.98"));

        cartWithItems = new CartFeignClient.CartDto(
                1L, 10L, List.of(item), new BigDecimal("1999.98"), 2);

        emptyCart = new CartFeignClient.CartDto(
                1L, 10L, List.of(), BigDecimal.ZERO, 0);

        OrderItem orderItem = OrderItem.builder()
                .productId(100L).productName("Laptop")
                .unitPrice(new BigDecimal("999.99")).quantity(2).build();

        savedOrder = Order.builder()
                .id(1L).userId(10L)
                .status(OrderStatus.PENDING_PAYMENT)
                .totalAmount(new BigDecimal("1999.98"))
                .shippingAddress("Istanbul")
                .items(new ArrayList<>(List.of(orderItem)))
                .build();
    }

    @Test
    void givenCartWithItems_whenCreateOrder_thenOrderCreatedAndCartCleared() {
        when(cartFeignClient.getCart(10L))
                .thenReturn(new ApiResponse<>(true, "ok", cartWithItems, null));
        when(cartFeignClient.clearCart(10L))
                .thenReturn(new ApiResponse<>(true, "cleared", null, null));
        when(orderRepository.save(any())).thenReturn(savedOrder);

        OrderResponse response = orderService.createOrder(10L, new CreateOrderRequest("Istanbul"));

        assertThat(response.status()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(response.totalAmount()).isEqualByComparingTo("1999.98");
        verify(cartFeignClient).clearCart(10L);
        verify(eventPublisher).publishOrderCreated(any(), eq(10L), any());
    }

    @Test
    void givenEmptyCart_whenCreateOrder_thenThrowBusinessException() {
        when(cartFeignClient.getCart(10L))
                .thenReturn(new ApiResponse<>(true, "ok", emptyCart, null));

        assertThatThrownBy(() -> orderService.createOrder(10L, new CreateOrderRequest("Istanbul")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("empty");
    }

    @Test
    void givenCartFeignError_whenCreateOrder_thenThrowBusinessException() {
        when(cartFeignClient.getCart(10L))
                .thenReturn(ApiResponse.error("Service unavailable"));

        assertThatThrownBy(() -> orderService.createOrder(10L, new CreateOrderRequest("Istanbul")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("cart");
    }

    @Test
    void givenExistingOrder_whenGetById_thenReturnOrderResponse() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(savedOrder));

        OrderResponse response = orderService.getById(1L, 10L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.userId()).isEqualTo(10L);
    }

    @Test
    void givenOrderBelongingToOtherUser_whenGetById_thenThrowBusinessException() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(savedOrder));

        assertThatThrownBy(() -> orderService.getById(1L, 99L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Access denied");
    }

    @Test
    void givenNonExistentOrder_whenGetById_thenThrowResourceNotFoundException() {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getById(999L, 10L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void givenPendingOrder_whenCancel_thenStatusCancelled() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(savedOrder));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse response = orderService.cancel(1L, 10L);

        assertThat(response.status()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void givenPaidOrder_whenCancel_thenThrowBusinessException() {
        savedOrder.setStatus(OrderStatus.PAID);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(savedOrder));

        assertThatThrownBy(() -> orderService.cancel(1L, 10L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("PENDING_PAYMENT");
    }

    @Test
    void givenPaidOrder_whenShip_thenStatusShippedAndEventPublished() {
        savedOrder.setStatus(OrderStatus.PAID);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(savedOrder));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse response = orderService.ship(1L, "TRK-12345");

        assertThat(response.status()).isEqualTo(OrderStatus.SHIPPED);
        assertThat(response.trackingNumber()).isEqualTo("TRK-12345");
        verify(eventPublisher).publishOrderShipped(1L, 10L, "TRK-12345");
    }

    @Test
    void givenPendingOrder_whenShip_thenThrowBusinessException() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(savedOrder));

        assertThatThrownBy(() -> orderService.ship(1L, "TRK-12345"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("PAID");
    }
}
