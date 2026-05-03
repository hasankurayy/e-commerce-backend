package com.ecommerce.product.service;

import com.ecommerce.common.dto.PageResponse;
import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.product.dto.ProductRequest;
import com.ecommerce.product.dto.ProductResponse;
import com.ecommerce.product.entity.Category;
import com.ecommerce.product.entity.Product;
import com.ecommerce.product.repository.CategoryRepository;
import com.ecommerce.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public PageResponse<ProductResponse> getAll(int page, int size, String sortBy) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy).descending());
        Page<Product> products = productRepository.findByActiveTrue(pageable);
        return toPageResponse(products);
    }

    public PageResponse<ProductResponse> getByCategory(Long categoryId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Product> products = productRepository.findByCategoryIdAndActiveTrue(categoryId, pageable);
        return toPageResponse(products);
    }

    public PageResponse<ProductResponse> search(String query, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        Page<Product> products = productRepository.findByNameContainingIgnoreCaseAndActiveTrue(query, pageable);
        return toPageResponse(products);
    }

    public ProductResponse getById(Long id) {
        return toResponse(findProduct(id));
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        Category category = request.categoryId() != null
                ? categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", request.categoryId()))
                : null;

        Product product = Product.builder()
                .name(request.name())
                .description(request.description())
                .price(request.price())
                .stock(request.stock())
                .imageUrl(request.imageUrl())
                .category(category)
                .active(true)
                .build();

        return toResponse(productRepository.save(product));
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = findProduct(id);
        Category category = request.categoryId() != null
                ? categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", request.categoryId()))
                : null;

        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setStock(request.stock());
        product.setImageUrl(request.imageUrl());
        product.setCategory(category);

        return toResponse(productRepository.save(product));
    }

    @Transactional
    public void delete(Long id) {
        Product product = findProduct(id);
        product.setActive(false);
        productRepository.save(product);
    }

    @Transactional
    public void decreaseStock(Long id, int quantity) {
        Product product = findProduct(id);
        if (product.getStock() < quantity) {
            throw new com.ecommerce.common.exception.BusinessException(
                "Insufficient stock for product: " + product.getName());
        }
        product.setStock(product.getStock() - quantity);
        productRepository.save(product);
    }

    @Transactional
    public void increaseStock(Long id, int quantity) {
        Product product = findProduct(id);
        product.setStock(product.getStock() + quantity);
        productRepository.save(product);
    }

    private Product findProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
    }

    private PageResponse<ProductResponse> toPageResponse(Page<Product> page) {
        var content = page.getContent().stream().map(this::toResponse).toList();
        return PageResponse.of(content, page.getNumber(), page.getSize(), page.getTotalElements());
    }

    private ProductResponse toResponse(Product p) {
        return new ProductResponse(
                p.getId(), p.getName(), p.getDescription(), p.getPrice(),
                p.getStock(), p.getImageUrl(),
                p.getCategory() != null ? p.getCategory().getId() : null,
                p.getCategory() != null ? p.getCategory().getName() : null,
                p.isActive(), p.getCreatedAt()
        );
    }
}
