package com.eshop.app.dto.response;

import com.eshop.app.entity.Coupon;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Coupon response DTO for API responses
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponResponse {
    
    private Long id;
    private String code;
    private String name;
    private String description;
    private Coupon.DiscountType discountType;
    private BigDecimal discountValue;
    private BigDecimal minimumOrderAmount;
    private BigDecimal maximumDiscountAmount;
    private Integer usageLimit;
    private Integer usageLimitPerUser;
    private Integer usedCount;
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;
    private Boolean isActive;
    private Coupon.AppliesTo appliesTo;
    private Long storeId;
    private String storeName;
    private Long categoryId;
    private String categoryName;
    private Boolean firstTimeOnly;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Additional computed fields
    private Integer remainingUses;
    private Boolean isExpiringSoon;
    private String discountDisplay; // e.g., "10% OFF" or "$5 OFF"
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValidationResult {
        private Boolean isValid;
        private String message;
        private BigDecimal discountAmount;
        private String errorCode;
        private CouponResponse coupon;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApplicationResult {
        private Boolean success;
        private String message;
        private BigDecimal appliedDiscount;
        private BigDecimal finalTotal;
        private String couponCode;
        private CouponResponse coupon;
    }
}