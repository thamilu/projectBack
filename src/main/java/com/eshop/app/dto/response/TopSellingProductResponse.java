package com.eshop.app.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Top selling product summary DTO.
 * 
 * <p>Provides sales performance metrics for high-performing products.
 * Used in analytics dashboards and reporting.
 * 
 * <p>Performance characteristics:
 * <ul>
 *   <li>Immutable after creation (use Builder pattern)</li>
 *   <li>Typically cached with TTL of 5-15 minutes</li>
 *   <li>Aggregated from OrderItem entities</li>
 * </ul>
 * 
 * @since 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Top selling product with sales metrics")
public class TopSellingProductResponse {
    
    // ═══════════════════════════════════════════════════════════════
    // PRODUCT IDENTITY
    // ═══════════════════════════════════════════════════════════════
    
    @Schema(description = "Product ID", example = "101", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long productId;
    
    @Schema(description = "Product name", example = "iPhone 15 Pro Max", requiredMode = Schema.RequiredMode.REQUIRED)
    private String productName;
    
    @Schema(description = "Product SKU", example = "AAPL-IP15PM-256-BLK", requiredMode = Schema.RequiredMode.REQUIRED)
    private String sku;
    
    @Schema(description = "Product friendly URL", example = "iphone-15-pro-max-256gb-black")
    private String friendlyUrl;
    
    // ═══════════════════════════════════════════════════════════════
    // PRODUCT ATTRIBUTES
    // ═══════════════════════════════════════════════════════════════
    
    @Schema(description = "Category name", example = "Smartphones")
    private String categoryName;
    
    @Schema(description = "Brand name", example = "Apple")
    private String brandName;
    
    @Schema(description = "Current price", example = "1199.99", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal currentPrice;
    
    @Schema(description = "Discount price (if applicable)", example = "1099.99")
    private BigDecimal discountPrice;
    
    @Schema(description = "Product image URL")
    private String imageUrl;
    
    // ═══════════════════════════════════════════════════════════════
    // SALES METRICS
    // ═══════════════════════════════════════════════════════════════
    
    @Schema(
        description = "Total quantity sold", 
        example = "1250",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Long totalQuantitySold;
    
    @Schema(
        description = "Total revenue generated", 
        example = "1499875.00",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private BigDecimal totalRevenue;
    
    @Schema(description = "Average order quantity", example = "1.8")
    private Double averageOrderQuantity;
    
    @Schema(description = "Number of unique orders", example = "694")
    private Long uniqueOrderCount;
    
    // ═══════════════════════════════════════════════════════════════
    // RANKING & PERFORMANCE
    // ═══════════════════════════════════════════════════════════════
    
    @Schema(
        description = "Sales rank (1 = best seller)", 
        example = "1",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Integer rank;
    
    @Schema(description = "Percentage of total platform sales", example = "8.5")
    private Double salesPercentage;
    
    @Schema(description = "Average customer rating", example = "4.7")
    private Double averageRating;
    
    @Schema(description = "Total reviews", example = "450")
    private Long reviewCount;
    
    // ═══════════════════════════════════════════════════════════════
    // INVENTORY STATUS
    // ═══════════════════════════════════════════════════════════════
    
    @Schema(description = "Current stock quantity", example = "150")
    private Integer currentStock;
    
    @Schema(description = "Stock status indicator", example = "IN_STOCK", allowableValues = {"IN_STOCK", "LOW_STOCK", "OUT_OF_STOCK"})
    private String stockStatus;
    
    // ═══════════════════════════════════════════════════════════════
    // SELLER INFORMATION
    // ═══════════════════════════════════════════════════════════════
    
    @Schema(description = "Seller ID", example = "42")
    private Long sellerId;
    
    @Schema(description = "Shop name", example = "Premium Tech Store")
    private String storeName;
}
