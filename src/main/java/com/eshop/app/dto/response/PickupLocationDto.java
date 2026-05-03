package com.eshop.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PickupLocationDto {
    private Long storeId;
    private String storeName;
    private String address;
    private CoordinatesDto coordinates;
    private Integer stockQuantity;
    private Boolean available;
    private java.util.Map<String, java.util.Map<String, String>> pickupHours;
}
