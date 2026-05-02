package com.ecommerce.product.service;

import com.ecommerce.common.dto.PageResponse;
import com.ecommerce.common.exception.BusinessException;
import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.product.dto.ProductResponse;
import com.ecommerce.product.entity.Category;
import com.ecommerce.product.entity.Product;
import com.ecommerce.product.repository.CategoryRepository;
import com.ecommerce.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock private ProductRepository productRepository;
    @Mock private CategoryRepository categoryRepository;

    @InjectMocks
    private ProductService productService;

    private Product testProduct;

    @BeforeEach
    void setUp() {
        Category category = Category.builder().id(1L).name("Electronics").build();
        testProduct = Product.builder()
                .id(1L).name("Laptop").price(new BigDecimal("999.99"))
                .stock(10).active(true).category(category).build();
    }

    @Test
    void givenNonExistentId_whenGetById_thenThrowResourceNotFoundException() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> productService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void givenValidId_whenGetById_thenReturnProductResponse() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        ProductResponse response = productService.getById(1L);
        assertThat(response.name()).isEqualTo("Laptop");
        assertThat(response.price()).isEqualByComparingTo("999.99");
    }

    @Test
    void givenInsufficientStock_whenDecreaseStock_thenThrowBusinessException() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        assertThatThrownBy(() -> productService.decreaseStock(1L, 99))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Insufficient stock");
    }

    @Test
    void givenSufficientStock_whenDecreaseStock_thenStockDecreased() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(productRepository.save(any())).thenReturn(testProduct);
        productService.decreaseStock(1L, 3);
        assertThat(testProduct.getStock()).isEqualTo(7);
    }

    @Test
    void givenProducts_whenGetAll_thenReturnPageResponse() {
        Page<Product> page = new PageImpl<>(List.of(testProduct), PageRequest.of(0, 20), 1);
        when(productRepository.findByActiveTrue(any())).thenReturn(page);
        PageResponse<ProductResponse> response = productService.getAll(0, 20, "createdAt");
        assertThat(response.content()).hasSize(1);
        assertThat(response.totalElements()).isEqualTo(1);
    }
}
