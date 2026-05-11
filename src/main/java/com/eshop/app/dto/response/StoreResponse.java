package com.eshop.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreResponse {
    
    private Long id;
    private String storeName;
    private String shopHandle;
    private String description;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String district;
    private String taluk;
    private String state;
    private String country;
    private String pincode;
    private String address;
    private String googleMapsUrl;
    private String phone;
    private String email;
    private String logoUrl;
    private String domain;
    private Boolean active;
    private Boolean isVerified;
    private Double rating;
    private Long totalRatings;
    private Long sellerId;
    private String sellerEmail;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
