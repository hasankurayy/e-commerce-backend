package com.ecommerce.notification.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "notification_logs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NotificationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String eventType;

    private Long orderId;
    private Long userId;
    private String recipient;
    private String subject;

    @Column(nullable = false)
    private boolean sent;

    private String errorMessage;

    @CreationTimestamp
    private Instant createdAt;
}
