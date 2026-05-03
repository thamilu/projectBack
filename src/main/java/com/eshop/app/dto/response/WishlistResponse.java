package com.eshop.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Wishlist response DTO for API responses
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WishlistResponse {

    private Long id;
    private Long userId;
    private Long productId;
    private String notes;
    private LocalDateTime createdAt;

    // Product details (when included)
    private ProductDetails product;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductDetails {
        private Long id;
        private String name;
        private String description;
        private BigDecimal price;
        private BigDecimal discountPrice;
        private String imageUrl;
        private Boolean isActive;
        private Integer stockQuantity;
        private Boolean inStock;
        private String storeName;
        private String categoryName;
        private Double averageRating;
        private Integer reviewCount;

        // Availability status
        private Boolean isAvailable;
        private String availabilityMessage;
    }
}