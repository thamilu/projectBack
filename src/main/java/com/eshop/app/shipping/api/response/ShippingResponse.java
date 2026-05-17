package com.eshop.app.shipping.api.response;

import com.eshop.app.shipping.domain.model.ShippingCarrier;
import com.eshop.app.shipping.domain.model.ShippingMethod;
import com.eshop.app.shipping.domain.model.ShippingStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Shipping response DTO for API responses
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShippingResponse {
    
    private Long id;
    private Long orderId;
    private String trackingNumber;
    private ShippingCarrier carrier;
    private ShippingMethod method;
    private ShippingStatus status;
    private BigDecimal cost;
    private BigDecimal weightKg;
    private String dimensions;
    private Address shippingAddress;
    private LocalDateTime shippedAt;
    private LocalDateTime estimatedDeliveryDate;
    private LocalDateTime actualDeliveryDate;
    private String deliveryInstructions;
    private Boolean signatureRequired;
    private String deliveredTo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Additional tracking information
    private String trackingUrl;
    private String currentLocation;
    private String statusMessage;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Address {
        private String fullName;
        private String addressLine1;
        private String addressLine2;
        private String city;
        private String state;
        private String postalCode;
        private String country;
        private String phoneNumber;
    }
}
