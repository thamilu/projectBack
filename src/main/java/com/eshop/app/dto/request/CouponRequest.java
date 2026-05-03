package com.eshop.app.dto.request;

import com.eshop.app.entity.Coupon;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Coupon request DTO for creating and updating coupons
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponRequest {
    
    @NotBlank(message = "Coupon code is required")
    @Size(min = 3, max = 50, message = "Code must be between 3 and 50 characters")
    @Pattern(regexp = "^[A-Z0-9_-]+$", message = "Code can only contain uppercase letters, numbers, hyphens and underscores")
    private String code;
    
    @NotBlank(message = "Coupon name is required")
    @Size(max = 100, message = "Name cannot exceed 100 characters")
    private String name;
    
    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;
    
    @NotNull(message = "Discount type is required")
    private Coupon.DiscountType discountType;
    
    @NotNull(message = "Discount value is required")
    @DecimalMin(value = "0.01", message = "Discount value must be greater than 0")
    @Digits(integer = 8, fraction = 2, message = "Discount value format is invalid")
    private BigDecimal discountValue;
    
    @DecimalMin(value = "0.01", message = "Minimum order amount must be greater than 0")
    @Digits(integer = 8, fraction = 2, message = "Minimum order amount format is invalid")
    private BigDecimal minimumOrderAmount;
    
    @DecimalMin(value = "0.01", message = "Maximum discount amount must be greater than 0")
    @Digits(integer = 8, fraction = 2, message = "Maximum discount amount format is invalid")
    private BigDecimal maximumDiscountAmount;
    
    @Min(value = 1, message = "Usage limit must be at least 1")
    private Integer usageLimit;
    
    @Min(value = 1, message = "Usage limit per user must be at least 1")
    private Integer usageLimitPerUser;
    
    @NotNull(message = "Valid from date is required")
    @Future(message = "Valid from date must be in the future")
    private LocalDateTime validFrom;
    
    @NotNull(message = "Valid until date is required")
    @Future(message = "Valid until date must be in the future")
    private LocalDateTime validUntil;
    
    @NotNull(message = "Applies to is required")
    private Coupon.AppliesTo appliesTo;
    
    private Long storeId; // For store-specific coupons
    private Long categoryId; // For category-specific coupons
    
    @Builder.Default
    private Boolean firstTimeOnly = false;
    
    /**
     * Validate that validUntil is after validFrom
     */
    @AssertTrue(message = "Valid until date must be after valid from date")
    public boolean isValidDateRange() {
        if (validFrom == null || validUntil == null) {
            return true; // Let other validations handle null checks
        }
        return validUntil.isAfter(validFrom);
    }
    
    /**
     * Validate discount value based on type
     */
    @AssertTrue(message = "Percentage discount must be between 1 and 100")
    public boolean isValidDiscountValue() {
        if (discountType == null || discountValue == null) {
            return true; // Let other validations handle null checks
        }
        
        if (discountType == Coupon.DiscountType.PERCENTAGE) {
            return discountValue.compareTo(BigDecimal.ONE) >= 0 && 
                   discountValue.compareTo(BigDecimal.valueOf(100)) <= 0;
        }
        
        return true; // Fixed amount can be any positive value
    }
}