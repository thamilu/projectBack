package com.eshop.app.dto.request;

import com.eshop.app.entity.Shipping;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Tracking update request DTO for updating shipping status
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrackingUpdateRequest {
    
    @NotBlank(message = "Tracking number is required")
    private String trackingNumber;
    
    @NotNull(message = "Status is required")
    private Shipping.ShippingStatus status;
    
    @Size(max = 500, message = "Location cannot exceed 500 characters")
    private String location;
    
    @Size(max = 1000, message = "Notes cannot exceed 1000 characters")
    private String notes;
    
    private String estimatedDeliveryDate; // ISO 8601 format
}