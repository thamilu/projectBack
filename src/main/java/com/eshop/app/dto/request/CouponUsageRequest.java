package com.eshop.app.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Coupon usage request DTO for applying coupons to orders
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponUsageRequest {
    
    @NotBlank(message = "Coupon code is required")
    private String couponCode;
    
    @NotNull(message = "User ID is required")
    private Long userId;
    
    @NotNull(message = "Order total is required")
    @DecimalMin(value = "0.01", message = "Order total must be greater than 0")
    @Digits(integer = 10, fraction = 2, message = "Order total format is invalid")
    private BigDecimal orderTotal;
    
    private Long storeId; // For store-specific validation
    private Long categoryId; // For category-specific validation
    private Long orderId; // For tracking coupon usage
}