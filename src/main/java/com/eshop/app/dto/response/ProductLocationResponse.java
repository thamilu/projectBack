package com.eshop.app.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductLocationResponse {
    @Schema(description = "Product id")
    private Long productId;

    @Schema(description = "Product name")
    private String productName;

    @Schema(description = "Product short description")
    private String shortDescription;

    @Schema(description = "Product price")
    private BigDecimal price;

    @Schema(description = "Currency code")
    private String currency;

    @Schema(description = "Store id where product is offered")
    private Long storeId;
 
    @Schema(description = "Store name")
    private String storeName;
 
    @Schema(description = "Latitude of store")
    private Double storeLatitude;
 
    @Schema(description = "Longitude of store")
    private Double storeLongitude;

    @Schema(description = "Distance in kilometers from search origin")
    private Double distanceKm;

    @Schema(description = "Availability flag")
    private Boolean inStock;

    @Schema(description = "Store information")
    private StoreLocationInfo store;

    @Schema(description = "Distance from user in miles", example = "1.55")
    private Double distanceMiles;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Store location information")
    public static class StoreLocationInfo {
        @Schema(description = "Store ID", example = "1")
        private Long id;
 
        @Schema(description = "Store name", example = "Tech Store SF")
        private String storeName;
 
        @Schema(description = "Store address")
        private String address;
 
        @Schema(description = "Store phone", example = "+1-555-0123")
        private String phone;
 
        @Schema(description = "Store latitude", example = "37.7749")
        private Double latitude;
 
        @Schema(description = "Store longitude", example = "-122.4194")
        private Double longitude;
 
        @Schema(description = "City", example = "San Francisco")
        private String city;
 
        @Schema(description = "State", example = "California")
        private String state;
 
        @Schema(description = "Country", example = "USA")
        private String country;
 
        @Schema(description = "Postal code", example = "94102")
        private String postalCode;
 
        @Schema(description = "Google Place ID")
        private String placeId;
    }
}
