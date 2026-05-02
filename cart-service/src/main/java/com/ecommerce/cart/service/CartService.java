package com.ecommerce.cart.service;

import com.ecommerce.cart.client.ProductFeignClient;
import com.ecommerce.cart.dto.AddItemRequest;
import com.ecommerce.cart.dto.CartResponse;
import com.ecommerce.cart.entity.Cart;
import com.ecommerce.cart.entity.CartItem;
import com.ecommerce.cart.repository.CartRepository;
import com.ecommerce.common.exception.BusinessException;
import com.ecommerce.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final ProductFeignClient productFeignClient;

    public CartResponse getCart(Long userId) {
        Cart cart = getOrCreateCart(userId);
        return toResponse(cart);
    }

    @Transactional
    public CartResponse addItem(Long userId, AddItemRequest request) {
        var productResponse = productFeignClient.getProduct(request.productId());
        if (!productResponse.success() || productResponse.data() == null) {
            throw new ResourceNotFoundException("Product", request.productId());
        }

        var product = productResponse.data();
        if (!product.active()) {
            throw new BusinessException("Product is not available: " + product.name());
        }
        if (product.stock() < request.quantity()) {
            throw new BusinessException("Insufficient stock for: " + product.name());
        }

        Cart cart = getOrCreateCart(userId);

        cart.getItems().stream()
                .filter(i -> i.getProductId().equals(request.productId()))
                .findFirst()
                .ifPresentOrElse(
                        existing -> existing.setQuantity(existing.getQuantity() + request.quantity()),
                        () -> cart.getItems().add(CartItem.builder()
                                .cart(cart)
                                .productId(product.id())
                                .productName(product.name())
                                .unitPrice(product.price())
                                .quantity(request.quantity())
                                .build())
                );

        cartRepository.save(cart);
        return toResponse(cart);
    }

    @Transactional
    public CartResponse updateItem(Long userId, Long productId, int quantity) {
        Cart cart = getUserCart(userId);

        CartItem item = cart.getItems().stream()
                .filter(i -> i.getProductId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));

        if (quantity <= 0) {
            cart.getItems().remove(item);
        } else {
            item.setQuantity(quantity);
        }

        cartRepository.save(cart);
        return toResponse(cart);
    }

    @Transactional
    public CartResponse removeItem(Long userId, Long productId) {
        Cart cart = getUserCart(userId);
        cart.getItems().removeIf(i -> i.getProductId().equals(productId));
        cartRepository.save(cart);
        return toResponse(cart);
    }

    @Transactional
    public void clearCart(Long userId) {
        cartRepository.findByUserId(userId).ifPresent(cart -> {
            cart.getItems().clear();
            cartRepository.save(cart);
        });
    }

    private Cart getOrCreateCart(Long userId) {
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> cartRepository.save(Cart.builder().userId(userId).build()));
    }

    private Cart getUserCart(Long userId) {
        return cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found for user: " + userId));
    }

    private CartResponse toResponse(Cart cart) {
        List<CartResponse.CartItemResponse> itemResponses = cart.getItems().stream()
                .map(i -> new CartResponse.CartItemResponse(
                        i.getId(), i.getProductId(), i.getProductName(),
                        i.getUnitPrice(), i.getQuantity(), i.getSubtotal()))
                .toList();

        BigDecimal total = itemResponses.stream()
                .map(CartResponse.CartItemResponse::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int totalItems = cart.getItems().stream().mapToInt(CartItem::getQuantity).sum();

        return new CartResponse(cart.getId(), cart.getUserId(), itemResponses, total, totalItems);
    }
}
