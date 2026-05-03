package com.eshop.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Lightweight DTO for product list views.
 * Contains only essential fields to optimize performance and payload size.
 * 
 * Used by:
 * - GET /api/v1/products (list all)
 * - GET /api/v1/products/search
 * - GET /api/v1/products/category/{id}
 * - GET /api/v1/products/brand/{id}
 * 
 * For full product details, use GET /api/v1/products/{id} which returns ProductResponse.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductListResponse {
    private Long id;
    private String name;
    private BigDecimal price;
    private BigDecimal discountPrice;
    private String imageUrl;
    private String friendlyUrl;
    private Boolean featured;
    private Boolean active;
    private Integer stockQuantity;
    private String categoryName;
    private String brandName;
    private String storeName;
}
