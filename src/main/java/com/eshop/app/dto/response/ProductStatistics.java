package com.eshop.app.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Global product statistics DTO.
 * 
 * <p>Provides comprehensive platform-wide product metrics including:
 * <ul>
 *   <li>Total product counts (active/inactive)</li>
 *   <li>Inventory health metrics</li>
 *   <li>Price statistics</li>
 *   <li>Category distribution</li>
 * </ul>
 * 
 * @since 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Global product statistics and metrics")
public class ProductStatistics {
    
    // ═══════════════════════════════════════════════════════════════
    // COUNT METRICS
    // ═══════════════════════════════════════════════════════════════
    
    @Schema(description = "Total number of products", example = "1500")
    private Long totalProducts;
    
    @Schema(description = "Number of active products", example = "1320")
    private Long activeProducts;
    
    @Schema(description = "Number of inactive products", example = "180")
    private Long inactiveProducts;
    
    @Schema(description = "Number of featured products", example = "50")
    private Long featuredProducts;
    
    // ═══════════════════════════════════════════════════════════════
    // INVENTORY HEALTH
    // ═══════════════════════════════════════════════════════════════
    
    @Schema(description = "Number of out-of-stock products", example = "45")
    private Long outOfStockCount;
    
    @Schema(description = "Number of low-stock products (< 10 units)", example = "120")
    private Long lowStockCount;
    
    @Schema(description = "Total inventory units across all products", example = "125000")
    private Long totalInventoryUnits;
    
    @Schema(description = "Average stock quantity per product", example = "83.33")
    private Double averageStockPerProduct;
    
    // ═══════════════════════════════════════════════════════════════
    // PRICE STATISTICS
    // ═══════════════════════════════════════════════════════════════
    
    @Schema(description = "Average product price", example = "149.99")
    private BigDecimal averagePrice;
    
    @Schema(description = "Minimum product price", example = "9.99")
    private BigDecimal minPrice;
    
    @Schema(description = "Maximum product price", example = "9999.99")
    private BigDecimal maxPrice;
    
    @Schema(description = "Total value of all inventory", example = "18749875.00")
    private BigDecimal totalInventoryValue;
    
    // ═══════════════════════════════════════════════════════════════
    // CATEGORY DISTRIBUTION
    // ═══════════════════════════════════════════════════════════════
    
    @Schema(description = "Number of unique categories", example = "25")
    private Long totalCategories;
    
    @Schema(description = "Number of unique brands", example = "150")
    private Long totalBrands;
    
    @Schema(description = "Number of unique tags", example = "300")
    private Long totalTags;
    
    // ═══════════════════════════════════════════════════════════════
    // TEMPORAL METADATA
    // ═══════════════════════════════════════════════════════════════
    
    @Schema(description = "Timestamp when statistics were generated")
    private LocalDateTime generatedAt;
    
    @Schema(description = "Products added in last 24 hours", example = "15")
    private Long productsAddedLast24Hours;
    
    @Schema(description = "Products updated in last 24 hours", example = "45")
    private Long productsUpdatedLast24Hours;
}
