package com.eshop.app.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Lightweight product DTO for list views and search results.
 * 
 * <p>Optimized for performance with minimal fields.
 * Use this for:
 * <ul>
 *   <li>Product listing pages</li>
 *   <li>Search results</li>
 *   <li>Category browse pages</li>
 *   <li>Related products widgets</li>
 * </ul>
 * 
 * @author E-Shop Team
 * @version 2.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Lightweight product summary for list views")
public class ProductSummaryResponse {
    
    @Schema(description = "Product ID", example = "1")
    private Long id;
    
    @Schema(description = "Product name", example = "iPhone 15 Pro", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;
    
    @Schema(description = "Product SKU", example = "AAPL-IP15-PRO")
    private String sku;
    
    @Schema(description = "SEO-friendly URL", example = "iphone-15-pro")
    private String friendlyUrl;
    
    @Schema(description = "Regular price", example = "999.99")
    private BigDecimal price;
    
    @Schema(description = "Discounted price", example = "899.99")
    private BigDecimal discountPrice;
    
    @Schema(description = "Product image URL")
    private String imageUrl;
    
    @Schema(description = "Whether product is active", example = "true")
    private Boolean active;
    
    @Schema(description = "Whether product is featured", example = "false")
    private Boolean featured;
    
    @Schema(description = "Stock quantity", example = "50")
    private Integer stockQuantity;
    
    @Schema(description = "Whether product is in stock", example = "true")
    private Boolean inStock;
    
    @Schema(description = "Category name", example = "Smartphones")
    private String categoryName;
    
    @Schema(description = "Brand name", example = "Apple")
    private String brandName;
    
    @Schema(description = "Average rating", example = "4.5")
    private Double averageRating;
    
    @Schema(description = "Number of reviews", example = "127")
    private Long reviewCount;
    
    /**
     * Gets the effective price (discount price if available, otherwise regular price).
     */
    public BigDecimal getEffectivePrice() {
        return discountPrice != null && discountPrice.compareTo(price) < 0 
            ? discountPrice 
            : price;
    }
    
    /**
     * Checks if the product has an active discount.
     */
    public boolean hasDiscount() {
        return discountPrice != null && 
               price != null && 
               discountPrice.compareTo(price) < 0;
    }
}
