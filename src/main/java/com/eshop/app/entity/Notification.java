package com.eshop.app.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Notification entity for managing user notifications
 * Supports email, SMS, and push notifications
 * 
 * Time Complexity: O(1) for creation, O(log n) for queries with indexing
 * Space Complexity: O(1) per notification record
 */
@Entity
@Table(name = "notifications", indexes = {
    @Index(name = "idx_notification_user_id", columnList = "user_id"),
    @Index(name = "idx_notification_type", columnList = "type"),
    @Index(name = "idx_notification_status", columnList = "status"),
    @Index(name = "idx_notification_created_at", columnList = "created_at"),
    @Index(name = "idx_notification_scheduled_at", columnList = "scheduled_at")
})
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(name = "type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private NotificationType type;
    
    @Column(name = "channel", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private NotificationChannel channel;
    
    @Column(name = "title", nullable = false, length = 200)
    private String title;
    
    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;
    
    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private NotificationStatus status;
    
    @Column(name = "priority", nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    private NotificationPriority priority;
    
    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;
    
    @Column(name = "sent_at")
    private LocalDateTime sentAt;
    
    @Column(name = "read_at")
    private LocalDateTime readAt;
    
    @Column(name = "template_id", length = 100)
    private String templateId;
    
    @Column(name = "template_data", columnDefinition = "TEXT")
    private String templateData; // JSON format for template variables
    
    @Column(name = "recipient_email", length = 255)
    private String recipientEmail;
    
    @Column(name = "recipient_phone", length = 20)
    private String recipientPhone;
    
    @Column(name = "push_token", length = 500)
    private String pushToken;
    
    @Column(name = "external_id", length = 100)
    private String externalId; // ID from external service (SendGrid, Twilio, etc.)
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    @Column(name = "retry_count", columnDefinition = "INTEGER DEFAULT 0")
    @Builder.Default
    private Integer retryCount = 0;
    
    @Column(name = "max_retries", columnDefinition = "INTEGER DEFAULT 3")
    @Builder.Default
    private Integer maxRetries = 3;
    
    // Reference fields for context
    @Column(name = "order_id")
    private Long orderId;
    
    @Column(name = "product_id")
    private Long productId;
    
    @Column(name = "shop_id")
    private Long shopId;
    
    // Notification enums
    public enum NotificationType {
        ORDER_CONFIRMATION,
        ORDER_SHIPPED,
        ORDER_DELIVERED,
        ORDER_CANCELLED,
        PAYMENT_SUCCESS,
        PAYMENT_FAILED,
        PAYMENT_REFUNDED,
        PRODUCT_BACK_IN_STOCK,
        PRODUCT_PRICE_DROP,
        COUPON_EXPIRING,
        ACCOUNT_WELCOME,
        ACCOUNT_VERIFICATION,
        PASSWORD_RESET,
        SECURITY_ALERT,
        PROMOTIONAL,
        SYSTEM_MAINTENANCE,
        CUSTOM
    }
    
    public enum NotificationChannel {
        EMAIL,
        SMS,
        PUSH,
        IN_APP
    }
    
    public enum NotificationStatus {
        PENDING,    // Created but not sent
        SCHEDULED,  // Scheduled for future delivery
        SENT,       // Successfully sent
        DELIVERED,  // Confirmed delivery (for some channels)
        FAILED,     // Failed to send
        CANCELLED   // Cancelled before sending
    }
    
    public enum NotificationPriority {
        LOW,
        NORMAL,
        HIGH,
        URGENT
    }
    
    /**
     * Check if notification can be retried
     */
    public boolean canRetry() {
        return status == NotificationStatus.FAILED && retryCount < maxRetries;
    }
    
    /**
     * Mark notification as sent
     */
    public void markAsSent(String externalId) {
        this.status = NotificationStatus.SENT;
        this.sentAt = LocalDateTime.now();
        this.externalId = externalId;
    }
    
    /**
     * Mark notification as failed with error
     */
    public void markAsFailed(String errorMessage) {
        this.status = NotificationStatus.FAILED;
        this.errorMessage = errorMessage;
        this.retryCount++;
    }
    
    /**
     * Mark notification as read by user
     */
    public void markAsRead() {
        this.readAt = LocalDateTime.now();
    }
    
    /**
     * Check if notification is read
     */
    public boolean isRead() {
        return readAt != null;
    }
}