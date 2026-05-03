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
 * Shipping entity for managing delivery information
 * Supports multiple shipping methods and carriers
 * 
 * Time Complexity: O(1) for all operations with proper indexing
 * Space Complexity: O(1) per shipping record
 */
@Entity
@Table(name = "shippings", indexes = {
    @Index(name = "idx_shipping_order_id", columnList = "order_id"),
    @Index(name = "idx_shipping_tracking_number", columnList = "tracking_number", unique = true),
    @Index(name = "idx_shipping_status", columnList = "status"),
    @Index(name = "idx_shipping_carrier", columnList = "carrier"),
    @Index(name = "idx_shipping_delivery_date", columnList = "estimated_delivery_date")
})
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Shipping extends BaseEntity {
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;
    
    @Column(name = "tracking_number", unique = true, length = 100)
    private String trackingNumber;
    
    @Column(name = "carrier", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private ShippingCarrier carrier;
    
    @Column(name = "method", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private ShippingMethod method;
    
    @Column(name = "status", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private ShippingStatus status;
    
    @Column(name = "cost", nullable = false, precision = 10, scale = 2)
    private BigDecimal cost;
    
    @Column(name = "weight_kg", precision = 8, scale = 3)
    private BigDecimal weightKg;
    
    @Column(name = "dimensions", length = 100)
    private String dimensions; // Format: "length x width x height"
    
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "fullName", column = @Column(name = "shipping_full_name")),
        @AttributeOverride(name = "addressLine1", column = @Column(name = "shipping_address_line1")),
        @AttributeOverride(name = "addressLine2", column = @Column(name = "shipping_address_line2")),
        @AttributeOverride(name = "city", column = @Column(name = "shipping_city")),
        @AttributeOverride(name = "state", column = @Column(name = "shipping_state")),
        @AttributeOverride(name = "postalCode", column = @Column(name = "shipping_postal_code")),
        @AttributeOverride(name = "country", column = @Column(name = "shipping_country")),
        @AttributeOverride(name = "phoneNumber", column = @Column(name = "shipping_phone_number"))
    })
    private Address shippingAddress;
    
    @Column(name = "shipped_at")
    private LocalDateTime shippedAt;
    
    @Column(name = "estimated_delivery_date")
    private LocalDateTime estimatedDeliveryDate;
    
    @Column(name = "actual_delivery_date")
    private LocalDateTime actualDeliveryDate;
    
    @Column(name = "delivery_instructions", columnDefinition = "TEXT")
    private String deliveryInstructions;
    
    @Column(name = "signature_required", columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean signatureRequired;
    
    @Column(name = "delivered_to", length = 100)
    private String deliveredTo;
    
    // Shipping enums
    public enum ShippingCarrier {
        UPS, FEDEX, DHL, USPS, LOCAL_DELIVERY, COURIER
    }
    
    public enum ShippingMethod {
        STANDARD,      // 5-7 business days
        EXPEDITED,     // 2-3 business days  
        OVERNIGHT,     // Next business day
        TWO_DAY,       // 2 business days
        SAME_DAY,      // Same day delivery
        PICKUP         // Customer pickup
    }
    
    public enum ShippingStatus {
        PENDING,           // Shipping label created but not shipped
        SHIPPED,          // Package shipped
        IN_TRANSIT,       // Package in transit
        OUT_FOR_DELIVERY, // Package out for delivery
        DELIVERED,        // Package delivered
        RETURNED,         // Package returned to sender
        LOST,             // Package lost
        DAMAGED           // Package damaged
    }
    
    /**
     * Address embeddable class for shipping addresses
     */
    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Address {
        @Column(name = "full_name", nullable = false, length = 100)
        private String fullName;
        
        @Column(name = "address_line1", nullable = false, length = 255)
        private String addressLine1;
        
        @Column(name = "address_line2", length = 255)
        private String addressLine2;
        
        @Column(name = "city", nullable = false, length = 100)
        private String city;
        
        @Column(name = "state", nullable = false, length = 100)
        private String state;
        
        @Column(name = "postal_code", nullable = false, length = 20)
        private String postalCode;
        
        @Column(name = "country", nullable = false, length = 100)
        private String country;
        
        @Column(name = "phone_number", length = 20)
        private String phoneNumber;
    }
    
    /**
     * Check if package is in transit
     */
    public boolean isInTransit() {
        return status == ShippingStatus.SHIPPED || 
               status == ShippingStatus.IN_TRANSIT || 
               status == ShippingStatus.OUT_FOR_DELIVERY;
    }
    
    /**
     * Check if delivery is completed
     */
    public boolean isDelivered() {
        return status == ShippingStatus.DELIVERED;
    }
    
    /**
     * Mark as shipped
     */
    public void markAsShipped(String trackingNumber) {
        this.trackingNumber = trackingNumber;
        this.status = ShippingStatus.SHIPPED;
        this.shippedAt = LocalDateTime.now();
    }
    
    /**
     * Mark as delivered
     */
    public void markAsDelivered(String deliveredTo) {
        this.status = ShippingStatus.DELIVERED;
        this.actualDeliveryDate = LocalDateTime.now();
        this.deliveredTo = deliveredTo;
    }
}