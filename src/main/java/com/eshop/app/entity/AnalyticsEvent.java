package com.eshop.app.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Analytics entity for storing business intelligence data
 * Optimized for reporting and dashboard queries
 * 
 * Time Complexity: O(1) for inserts, O(log n) for aggregate queries
 * Space Complexity: O(n) where n is number of data points
 */
@Entity
@Table(name = "analytics_events", indexes = {
    @Index(name = "idx_analytics_event_type", columnList = "event_type"),
    @Index(name = "idx_analytics_user_id", columnList = "user_id"),
    @Index(name = "idx_analytics_shop_id", columnList = "shop_id"),
    @Index(name = "idx_analytics_product_id", columnList = "product_id"),
    @Index(name = "idx_analytics_timestamp", columnList = "timestamp"),
    @Index(name = "idx_analytics_date", columnList = "event_date"),
    @Index(name = "idx_analytics_composite", columnList = "event_type,event_date")
})
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsEvent extends BaseEntity {
    
    @Column(name = "event_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private EventType eventType;
    
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;
    
    @Column(name = "event_date", nullable = false)
    private java.sql.Date eventDate; // For partitioning and daily aggregations
    
    @Column(name = "user_id")
    private Long userId;
    
    @Column(name = "shop_id")
    private Long shopId;
    
    @Column(name = "product_id")
    private Long productId;
    
    @Column(name = "order_id")
    private Long orderId;
    
    @Column(name = "category_id")
    private Long categoryId;
    
    // Numeric values for aggregation
    @Column(name = "revenue", precision = 12, scale = 2)
    private BigDecimal revenue;
    
    @Column(name = "quantity")
    private Integer quantity;
    
    @Column(name = "value", precision = 12, scale = 2)
    private BigDecimal value;
    
    // Session and tracking info
    @Column(name = "session_id", length = 100)
    private String sessionId;
    
    @Column(name = "ip_address", length = 45)
    private String ipAddress;
    
    @Column(name = "user_agent", length = 500)
    private String userAgent;
    
    @Column(name = "referrer_url", length = 500)
    private String referrerUrl;
    
    // Geographic info
    @Column(name = "country", length = 100)
    private String country;
    
    @Column(name = "city", length = 100)
    private String city;
    
    // Additional metadata
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata; // JSON format for additional event data
    
    // Analytics event types
    public enum EventType {
        // User behavior
        USER_REGISTRATION,
        USER_LOGIN,
        USER_LOGOUT,
        
        // Product interactions
        PRODUCT_VIEW,
        PRODUCT_SEARCH,
        PRODUCT_REVIEW_ADDED,
        WISHLIST_ADD,
        WISHLIST_REMOVE,
        
        // Shopping behavior
        CART_ADD,
        CART_REMOVE,
        CART_UPDATE,
        CHECKOUT_START,
        CHECKOUT_COMPLETE,
        
        // Orders
        ORDER_PLACED,
        ORDER_CANCELLED,
        ORDER_RETURNED,
        ORDER_DELIVERED,
        
        // Payments
        PAYMENT_SUCCESS,
        PAYMENT_FAILED,
        REFUND_PROCESSED,
        
        // Marketing
        COUPON_USED,
        NEWSLETTER_SIGNUP,
        PROMOTION_CLICKED,
        
        // Shop activities
        SHOP_VIEW,
        SHOP_FOLLOW,
        PRODUCT_LISTED,
        
        // System events
        EMAIL_SENT,
        SMS_SENT,
        PUSH_NOTIFICATION_SENT,
        
        // Custom events
        CUSTOM_EVENT
    }
    
    /**
     * Create user behavior event
     */
    public static AnalyticsEvent createUserEvent(EventType eventType, Long userId, String sessionId) {
        return AnalyticsEvent.builder()
                .eventType(eventType)
                .timestamp(LocalDateTime.now())
                .eventDate(java.sql.Date.valueOf(LocalDateTime.now().toLocalDate()))
                .userId(userId)
                .sessionId(sessionId)
                .build();
    }
    
    /**
     * Create product interaction event
     */
    public static AnalyticsEvent createProductEvent(EventType eventType, Long userId, Long productId, 
                                                   Long shopId, String sessionId) {
        return AnalyticsEvent.builder()
                .eventType(eventType)
                .timestamp(LocalDateTime.now())
                .eventDate(java.sql.Date.valueOf(LocalDateTime.now().toLocalDate()))
                .userId(userId)
                .productId(productId)
                .shopId(shopId)
                .sessionId(sessionId)
                .build();
    }
    
    /**
     * Create revenue event
     */
    public static AnalyticsEvent createRevenueEvent(EventType eventType, Long userId, Long orderId,
                                                   Long shopId, BigDecimal revenue, Integer quantity) {
        return AnalyticsEvent.builder()
                .eventType(eventType)
                .timestamp(LocalDateTime.now())
                .eventDate(java.sql.Date.valueOf(LocalDateTime.now().toLocalDate()))
                .userId(userId)
                .orderId(orderId)
                .shopId(shopId)
                .revenue(revenue)
                .quantity(quantity)
                .build();
    }
}