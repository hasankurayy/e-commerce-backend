package com.ecommerce.cart.service;

import com.ecommerce.cart.client.ProductFeignClient;
import com.ecommerce.cart.dto.AddItemRequest;
import com.ecommerce.cart.dto.CartResponse;
import com.ecommerce.cart.entity.Cart;
import com.ecommerce.cart.entity.CartItem;
import com.ecommerce.cart.repository.CartRepository;
import com.ecommerce.common.dto.ApiResponse;
import com.ecommerce.common.exception.BusinessException;
import com.ecommerce.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock private CartRepository cartRepository;
    @Mock private ProductFeignClient productFeignClient;

    @InjectMocks
    private CartService cartService;

    private Cart emptyCart;
    private ProductFeignClient.ProductDto availableProduct;

    @BeforeEach
    void setUp() {
        emptyCart = Cart.builder()
                .id(1L).userId(10L).items(new ArrayList<>()).build();

        availableProduct = new ProductFeignClient.ProductDto(
                100L, "Laptop", new BigDecimal("999.99"), 10, true);
    }

    @Test
    void givenEmptyCart_whenGetCart_thenReturnEmptyCartResponse() {
        when(cartRepository.findByUserId(10L)).thenReturn(Optional.of(emptyCart));

        CartResponse response = cartService.getCart(10L);

        assertThat(response.items()).isEmpty();
        assertThat(response.totalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.totalItems()).isEqualTo(0);
    }

    @Test
    void givenNoCart_whenGetCart_thenCreateAndReturnEmpty() {
        when(cartRepository.findByUserId(99L)).thenReturn(Optional.empty());
        when(cartRepository.save(any())).thenReturn(emptyCart);

        CartResponse response = cartService.getCart(99L);

        assertThat(response.items()).isEmpty();
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    void givenProductNotFound_whenAddItem_thenThrowResourceNotFoundException() {
        when(productFeignClient.getProduct(999L))
                .thenReturn(ApiResponse.error("Not found"));

        assertThatThrownBy(() -> cartService.addItem(10L, new AddItemRequest(999L, 1)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void givenInactiveProduct_whenAddItem_thenThrowBusinessException() {
        var inactiveProduct = new ProductFeignClient.ProductDto(
                100L, "OldProduct", new BigDecimal("50.00"), 5, false);

        when(productFeignClient.getProduct(100L))
                .thenReturn(new ApiResponse<>(true, "ok", inactiveProduct, null));

        assertThatThrownBy(() -> cartService.addItem(10L, new AddItemRequest(100L, 1)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("not available");
    }

    @Test
    void givenInsufficientStock_whenAddItem_thenThrowBusinessException() {
        when(productFeignClient.getProduct(100L))
                .thenReturn(new ApiResponse<>(true, "ok", availableProduct, null));

        assertThatThrownBy(() -> cartService.addItem(10L, new AddItemRequest(100L, 999)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Insufficient stock");
    }

    @Test
    void givenValidProduct_whenAddItem_thenItemAddedToCart() {
        when(productFeignClient.getProduct(100L))
                .thenReturn(new ApiResponse<>(true, "ok", availableProduct, null));
        when(cartRepository.findByUserId(10L)).thenReturn(Optional.of(emptyCart));
        when(cartRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CartResponse response = cartService.addItem(10L, new AddItemRequest(100L, 2));

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).productName()).isEqualTo("Laptop");
        assertThat(response.items().get(0).quantity()).isEqualTo(2);
        assertThat(response.totalAmount()).isEqualByComparingTo("1999.98");
    }

    @Test
    void givenSameProductAddedTwice_whenAddItem_thenQuantityIncreased() {
        CartItem existingItem = CartItem.builder()
                .id(1L).cart(emptyCart).productId(100L)
                .productName("Laptop").unitPrice(new BigDecimal("999.99")).quantity(1).build();
        emptyCart.getItems().add(existingItem);

        when(productFeignClient.getProduct(100L))
                .thenReturn(new ApiResponse<>(true, "ok", availableProduct, null));
        when(cartRepository.findByUserId(10L)).thenReturn(Optional.of(emptyCart));
        when(cartRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CartResponse response = cartService.addItem(10L, new AddItemRequest(100L, 2));

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).quantity()).isEqualTo(3); // 1 + 2
    }

    @Test
    void givenQuantityZero_whenUpdateItem_thenItemRemoved() {
        CartItem item = CartItem.builder()
                .id(1L).cart(emptyCart).productId(100L)
                .productName("Laptop").unitPrice(new BigDecimal("999.99")).quantity(2).build();
        emptyCart.getItems().add(item);

        when(cartRepository.findByUserId(10L)).thenReturn(Optional.of(emptyCart));
        when(cartRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CartResponse response = cartService.updateItem(10L, 100L, 0);

        assertThat(response.items()).isEmpty();
    }

    @Test
    void givenItemInCart_whenRemoveItem_thenItemRemoved() {
        CartItem item = CartItem.builder()
                .id(1L).cart(emptyCart).productId(100L)
                .productName("Laptop").unitPrice(new BigDecimal("999.99")).quantity(1).build();
        emptyCart.getItems().add(item);

        when(cartRepository.findByUserId(10L)).thenReturn(Optional.of(emptyCart));
        when(cartRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CartResponse response = cartService.removeItem(10L, 100L);

        assertThat(response.items()).isEmpty();
    }
}
