package com.ecommerce.order.repository;

import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Page<Order> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    Page<Order> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, OrderStatus status, Pageable pageable);
    Page<Order> findByUserIdAndStatusNotInOrderByCreatedAtDesc(Long userId, Collection<OrderStatus> statuses, Pageable pageable);
    List<Order> findByStatusAndUpdatedAtBefore(OrderStatus status, Instant before);

    @org.springframework.data.jpa.repository.Query(
        "SELECT COUNT(o) FROM Order o JOIN o.items i " +
        "WHERE o.userId = :userId AND o.status = :status AND i.productId = :productId")
    long countByUserIdAndStatusAndItemsProductId(
        @org.springframework.data.repository.query.Param("userId") Long userId,
        @org.springframework.data.repository.query.Param("status") OrderStatus status,
        @org.springframework.data.repository.query.Param("productId") Long productId);
}
