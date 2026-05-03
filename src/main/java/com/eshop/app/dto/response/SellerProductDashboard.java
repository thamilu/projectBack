package com.eshop.app.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Seller-specific product dashboard DTO.
 * 
 * <p>Provides comprehensive seller product analytics including:
 * <ul>
 *   <li>Product inventory overview</li>
 *   <li>Performance metrics</li>
 *   <li>Top performing products</li>
 *   <li>Stock health indicators</li>
 * </ul>
 * 
 * @since 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Seller product dashboard with analytics")
public class SellerProductDashboard {
    
    // ═══════════════════════════════════════════════════════════════
    // SELLER IDENTITY
    // ═══════════════════════════════════════════════════════════════
    
    @Schema(description = "Seller ID", example = "123")
    private Long sellerId;
    
    @Schema(description = "Seller name", example = "TechStore Electronics")
    private String sellerName;
    
    @Schema(description = "Shop ID", example = "456")
    private Long shopId;
    
    @Schema(description = "Shop name", example = "TechStore Main Branch")
    private String storeName;
    
    // ═══════════════════════════════════════════════════════════════
    // PRODUCT COUNTS
    // ═══════════════════════════════════════════════════════════════
    
    @Schema(description = "Total products owned by seller", example = "250")
    private Long totalProducts;
    
    @Schema(description = "Active products", example = "220")
    private Long activeProducts;
    
    @Schema(description = "Inactive products", example = "30")
    private Long inactiveProducts;
    
    @Schema(description = "Featured products", example = "15")
    private Long featuredProducts;
    
    // ═══════════════════════════════════════════════════════════════
    // INVENTORY HEALTH
    // ═══════════════════════════════════════════════════════════════
    
    @Schema(description = "Out of stock products", example = "12")
    private Long outOfStockCount;
    
    @Schema(description = "Low stock products (< 10 units)", example = "35")
    private Long lowStockCount;
    
    @Schema(description = "Total inventory units", example = "15000")
    private Long totalInventoryUnits;
    
    @Schema(description = "Average stock per product", example = "60.0")
    private Double averageStockPerProduct;
    
    // ═══════════════════════════════════════════════════════════════
    // REVENUE METRICS
    // ═══════════════════════════════════════════════════════════════
    
    @Schema(description = "Total inventory value", example = "375000.00")
    private BigDecimal totalInventoryValue;
    
    @Schema(description = "Average product price", example = "149.99")
    private BigDecimal averagePrice;
    
    @Schema(description = "Highest priced product value", example = "2999.99")
    private BigDecimal highestPrice;
    
    @Schema(description = "Lowest priced product value", example = "19.99")
    private BigDecimal lowestPrice;
    
    // ═══════════════════════════════════════════════════════════════
    // PERFORMANCE METRICS
    // ═══════════════════════════════════════════════════════════════
    
    @Schema(description = "Total sales count (all time)", example = "5420")
    private Long totalSalesCount;
    
    @Schema(description = "Total revenue (all time)", example = "812350.00")
    private BigDecimal totalRevenue;
    
    @Schema(description = "Average rating across all products", example = "4.35")
    private Double averageRating;
    
    @Schema(description = "Total reviews received", example = "1280")
    private Long totalReviews;
    
    // ═══════════════════════════════════════════════════════════════
    // TOP PRODUCTS
    // ═══════════════════════════════════════════════════════════════
    
    @Schema(description = "Top selling products list")
    private List<TopSellingProductResponse> topSellingProducts;
    
    @Schema(description = "Top rated products list")
    private List<TopRatedProduct> topRatedProducts;
    
    // ═══════════════════════════════════════════════════════════════
    // TEMPORAL DATA
    // ═══════════════════════════════════════════════════════════════
    
    @Schema(description = "Products added in last 30 days", example = "18")
    private Long productsAddedLast30Days;
    
    @Schema(description = "Products updated in last 30 days", example = "65")
    private Long productsUpdatedLast30Days;
    
    @Schema(description = "Dashboard generation timestamp")
    private LocalDateTime generatedAt;
    
    // ═══════════════════════════════════════════════════════════════
    // NESTED DTOS
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Top rated product summary.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "Top rated product summary")
    public static class TopRatedProduct {
        
        @Schema(description = "Product ID", example = "789")
        private Long productId;
        
        @Schema(description = "Product name", example = "Premium Wireless Headphones")
        private String productName;
        
        @Schema(description = "Product SKU", example = "WH-1000XM5")
        private String sku;
        
        @Schema(description = "Average rating", example = "4.8")
        private Double averageRating;
        
        @Schema(description = "Total reviews", example = "450")
        private Long reviewCount;
        
        @Schema(description = "Current price", example = "349.99")
        private BigDecimal price;
        
        @Schema(description = "Product image URL")
        private String imageUrl;
    }
}
