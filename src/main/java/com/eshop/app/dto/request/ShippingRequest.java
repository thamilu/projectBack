package com.eshop.app.dto.request;

import com.eshop.app.entity.Shipping;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Shipping request DTO for creating and managing shipping
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShippingRequest {
    
    @NotNull(message = "Order ID is required")
    private Long orderId;
    
    @NotNull(message = "Shipping carrier is required")
    private Shipping.ShippingCarrier carrier;
    
    @NotNull(message = "Shipping method is required")
    private Shipping.ShippingMethod method;
    
    @DecimalMin(value = "0.01", message = "Weight must be greater than 0")
    @Digits(integer = 5, fraction = 3, message = "Weight format is invalid")
    private BigDecimal weightKg;
    
    @Size(max = 100, message = "Dimensions cannot exceed 100 characters")
    private String dimensions; // Format: "length x width x height"
    
    @NotNull(message = "Shipping address is required")
    @Valid
    private Address shippingAddress;
    
    @Size(max = 1000, message = "Delivery instructions cannot exceed 1000 characters")
    private String deliveryInstructions;
    
    @Builder.Default
    private Boolean signatureRequired = false;
    
    @DecimalMin(value = "0.00", message = "Cost cannot be negative")
    @Digits(integer = 8, fraction = 2, message = "Cost format is invalid")
    private BigDecimal cost;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Address {
        @NotBlank(message = "Full name is required")
        @Size(max = 100, message = "Full name cannot exceed 100 characters")
        private String fullName;
        
        @NotBlank(message = "Address line 1 is required")
        @Size(max = 255, message = "Address line 1 cannot exceed 255 characters")
        private String addressLine1;
        
        @Size(max = 255, message = "Address line 2 cannot exceed 255 characters")
        private String addressLine2;
        
        @NotBlank(message = "City is required")
        @Size(max = 100, message = "City cannot exceed 100 characters")
        private String city;
        
        @NotBlank(message = "State is required")
        @Size(max = 100, message = "State cannot exceed 100 characters")
        private String state;
        
        @NotBlank(message = "Postal code is required")
        @Size(max = 20, message = "Postal code cannot exceed 20 characters")
        private String postalCode;
        
        @NotBlank(message = "Country is required")
        @Size(max = 100, message = "Country cannot exceed 100 characters")
        private String country;
        
        @Pattern(regexp = "^[+]?[0-9\\s\\-\\(\\)]{10,20}$", message = "Invalid phone number format")
        private String phoneNumber;
    }
}