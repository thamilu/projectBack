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
 * Coupon entity for discount management system
 * Supports percentage and fixed amount discounts with various conditions
 * 
 * Time Complexity: O(1) for validation and application
 * Space Complexity: O(1) per coupon record
 */
@Entity
@Table(name = "coupons", indexes = {
    @Index(name = "idx_coupon_code", columnList = "code", unique = true),
    @Index(name = "idx_coupon_status", columnList = "is_active"),
    @Index(name = "idx_coupon_valid_from", columnList = "valid_from"),
    @Index(name = "idx_coupon_valid_until", columnList = "valid_until"),
    @Index(name = "idx_coupon_store_id", columnList = "store_id")
})
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Coupon extends BaseEntity {
    
    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;
    
    @Column(name = "name", nullable = false, length = 100)
    private String name;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "discount_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private DiscountType discountType;
    
    @Column(name = "discount_value", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountValue;
    
    @Column(name = "minimum_order_amount", precision = 10, scale = 2)
    private BigDecimal minimumOrderAmount;
    
    @Column(name = "maximum_discount_amount", precision = 10, scale = 2)
    private BigDecimal maximumDiscountAmount;
    
    @Column(name = "usage_limit")
    private Integer usageLimit;
    
    @Column(name = "usage_limit_per_user")
    private Integer usageLimitPerUser;
    
    @Column(name = "used_count", columnDefinition = "INTEGER DEFAULT 0")
    @Builder.Default
    private Integer usedCount = 0;
    
    @Column(name = "valid_from", nullable = false)
    private LocalDateTime validFrom;
    
    @Column(name = "valid_until", nullable = false)
    private LocalDateTime validUntil;
    
    @Column(name = "is_active", nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
    @Builder.Default
    private Boolean isActive = true;
    
    @Column(name = "applies_to", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private AppliesTo appliesTo;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    private Store store; // For store-specific coupons
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category; // For category-specific coupons
    
    @Column(name = "first_time_only", columnDefinition = "BOOLEAN DEFAULT FALSE")
    @Builder.Default
    private Boolean firstTimeOnly = false;
    
    // Coupon enums
    public enum DiscountType {
        PERCENTAGE,     // Discount as percentage (e.g., 10%)
        FIXED_AMOUNT    // Discount as fixed amount (e.g., $5)
    }
    
    public enum AppliesTo {
        ALL_ORDERS,         // Apply to entire order
        SPECIFIC_CATEGORY,  // Apply to specific category
        SPECIFIC_STORE,      // Apply to specific store
        SHIPPING_ONLY       // Apply to shipping cost only
    }
    
    /**
     * Check if coupon is currently valid
     */
    public boolean isValid() {
        LocalDateTime now = LocalDateTime.now();
        return isActive && 
               now.isAfter(validFrom) && 
               now.isBefore(validUntil) &&
               (usageLimit == null || usedCount < usageLimit);
    }
    
    /**
     * Check if user can use this coupon
     */
    public boolean canBeUsedByUser(User user, int userUsageCount) {
        if (!isValid()) {
            return false;
        }
        
        if (firstTimeOnly && user.getCreatedAt().isBefore(validFrom.minusDays(1))) {
            return false; // Not a new user
        }
        
        if (usageLimitPerUser != null && userUsageCount >= usageLimitPerUser) {
            return false; // User exceeded usage limit
        }
        
        return true;
    }
    
    /**
     * Calculate discount amount for given order total
     */
    public BigDecimal calculateDiscount(BigDecimal orderTotal) {
        if (!isValid()) {
            return BigDecimal.ZERO;
        }
        
        if (minimumOrderAmount != null && orderTotal.compareTo(minimumOrderAmount) < 0) {
            return BigDecimal.ZERO; // Order doesn't meet minimum requirement
        }
        
        BigDecimal discount;
        
        if (discountType == DiscountType.PERCENTAGE) {
            discount = orderTotal.multiply(discountValue).divide(BigDecimal.valueOf(100));
        } else {
            discount = discountValue;
        }
        
        // Apply maximum discount limit if set
        if (maximumDiscountAmount != null && discount.compareTo(maximumDiscountAmount) > 0) {
            discount = maximumDiscountAmount;
        }
        
        // Discount cannot exceed order total
        if (discount.compareTo(orderTotal) > 0) {
            discount = orderTotal;
        }
        
        return discount;
    }
    
    /**
     * Use the coupon (increment usage count)
     */
    public void use() {
        this.usedCount++;
        if (usageLimit != null && usedCount >= usageLimit) {
            this.isActive = false;
        }
    }
}