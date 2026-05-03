package com.eshop.app.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "subscription_transactions", indexes = {
    @Index(name = "idx_subscription_transaction_subscription", columnList = "subscription_id"),
    @Index(name = "idx_subscription_transaction_status", columnList = "status"),
    @Index(name = "idx_subscription_transaction_date", columnList = "attempted_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionTransaction extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;
    
    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private TransactionStatus status;
    
    @Column(name = "transaction_id", length = 255)
    private String transactionId;
    
    @Column(name = "attempted_at", nullable = false)
    private LocalDateTime attemptedAt;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    @Column(name = "error_message", length = 1000)
    private String errorMessage;
    
    public enum TransactionStatus {
        SUCCESS,
        FAILED,
        PENDING,
        REFUNDED
    }
}
