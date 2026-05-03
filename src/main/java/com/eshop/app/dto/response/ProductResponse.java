package com.eshop.app.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class ProductResponse {
    private Long id;
    private String name;
    private String description;
    private String sku;
    private String friendlyUrl;
    private BigDecimal price;
    private BigDecimal discountPrice;
    private Integer stockQuantity;
    private String imageUrl;
    private Boolean active;
    private Boolean featured;
    private Long categoryId;
    private String categoryName;
    private Long brandId;
    private String brandName;
    private Long storeId;
    private String storeName;
    private List<ProductImageResponse> images;
    private List<ProductReviewResponse> reviews;
    private List<String> tags;
    private Double averageRating;
    private Long reviewCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String categoryType;
    private String subCategory;
    private BaseInfoDto baseInfo;
    private PricingDto pricing;
    private List<LocationPricingDto> locationBasedPricing;
    private AvailabilityDto availability;
    private ShippingRestrictionsDto shippingRestrictions;
    private InventoryDto inventory;
    private Object categoryAttributes;

    // Bean-style alias for compatibility with code that expects getRating()/setRating()
    public Double getRating() {
        return this.averageRating;
    }

    public void setRating(Double rating) {
        this.averageRating = rating;
    }

    // Backwards-compatibility aliases for code expecting record-style accessors
    public Long id() { return this.id; }
    public java.time.LocalDateTime updatedAt() { return this.updatedAt; }
}
