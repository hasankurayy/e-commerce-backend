package com.ecommerce.product.repository;

import com.ecommerce.product.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findTop5ByProductIdOrderByCreatedAtDesc(Long productId);
    Optional<Review> findByProductIdAndUserId(Long productId, Long userId);
    boolean existsByProductIdAndUserId(Long productId, Long userId);
    int countByProductId(Long productId);

    @Query("SELECT COALESCE(SUM(r.rating), 0) FROM Review r WHERE r.productId = :productId")
    long sumRatingByProductId(@Param("productId") Long productId);
}
