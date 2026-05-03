package com.eshop.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WarehouseDto {
    private Long warehouseId;
    private String name;
    private String country;
    private String countryCode;
    private String state;
    private String stateCode;
    private String city;
    private String zipCode;
    private String address;
    private CoordinatesDto coordinates;
    private Integer stockQuantity;
    private Boolean isDefault;
    private Boolean isActive;
}
