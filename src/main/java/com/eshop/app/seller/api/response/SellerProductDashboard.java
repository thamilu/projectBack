package com.eshop.app.seller.api.response;

import com.eshop.app.catalog.api.response.TopSellingProductResponse;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Seller-specific product dashboard DTO.
 *
 * <p>Provides comprehensive seller product analytics including:
 *
 * <ul>
 *   <li>Product inventory overview
 *   <li>Performance metrics
 *   <li>Top performing products
 *   <li>Stock health indicators
 * </ul>
 *
 * @since 1.0
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Seller product dashboard with analytics")
public class SellerProductDashboard implements Serializable {

    private static final long serialVersionUID = 1L;

    // ═══════════════════════════════════════════════════════════════
    // SELLER IDENTITY
    // ═══════════════════════════════════════════════════════════════

    /** Unique identifier of the seller. */
    @Schema(description = "Seller ID", example = "123")
    private Long sellerId;

    /** Display name of the seller. */
    @Schema(description = "Seller name", example = "TechStore Electronics")
    private String sellerName;

    /** Unique identifier of the seller's shop. */
    @Schema(description = "Shop ID", example = "456")
    private Long shopId;

    /** Display name of the seller's store. */
    @Schema(description = "Shop name", example = "TechStore Main Branch")
    private String storeName;

    // ═══════════════════════════════════════════════════════════════
    // PRODUCT COUNTS
    // ═══════════════════════════════════════════════════════════════

    /** Total number of products owned by the seller. */
    @Schema(description = "Total products owned by seller", example = "250")
    private Long totalProducts;

    /** Number of currently active (visible) products. */
    @Schema(description = "Active products", example = "220")
    private Long activeProducts;

    /** Number of inactive (hidden) products. */
    @Schema(description = "Inactive products", example = "30")
    private Long inactiveProducts;

    /** Number of products marked as featured. */
    @Schema(description = "Featured products", example = "15")
    private Long featuredProducts;

    // ═══════════════════════════════════════════════════════════════
    // INVENTORY HEALTH
    // ═══════════════════════════════════════════════════════════════

    /** Number of products with zero stock. */
    @Schema(description = "Out of stock products", example = "12")
    private Long outOfStockCount;

    /** Number of products with stock below 10 units. */
    @Schema(description = "Low stock products (< 10 units)", example = "35")
    private Long lowStockCount;

    /** Total units across all product inventory. */
    @Schema(description = "Total inventory units", example = "15000")
    private Long totalInventoryUnits;

    /** Average stock quantity per product. */
    @Schema(description = "Average stock per product", example = "60.0")
    private Double averageStockPerProduct;

    // ═══════════════════════════════════════════════════════════════
    // REVENUE METRICS
    // ═══════════════════════════════════════════════════════════════

    /** Total value of all inventory at current prices. */
    @Schema(description = "Total inventory value in seller's local currency", example = "375000.00")
    @MonetaryField
    private BigDecimal totalInventoryValue;

    /** Average price across all products. */
    @Schema(description = "Average product price in seller's local currency", example = "149.99")
    @MonetaryField
    private BigDecimal averagePrice;

    /** Price of the highest-priced product. */
    @Schema(description = "Highest priced product value in seller's local currency", example = "2999.99")
    @MonetaryField
    private BigDecimal highestPrice;

    /** Price of the lowest-priced product. */
    @Schema(description = "Lowest priced product value in seller's local currency", example = "19.99")
    @MonetaryField
    private BigDecimal lowestPrice;

    // ═══════════════════════════════════════════════════════════════
    // PERFORMANCE METRICS
    // ═══════════════════════════════════════════════════════════════

    /** Total number of sales across all products (all time). */
    @Schema(description = "Total sales count (all time)", example = "5420")
    private Long totalSalesCount;

    /** Total revenue earned across all products (all time). */
    @Schema(description = "Total revenue (all time) in seller's local currency", example = "812350.00")
    @MonetaryField
    private BigDecimal totalRevenue;

    /** Average customer rating across all products. */
    @Schema(description = "Average rating across all products", example = "4.35")
    private Double averageRating;

    /** Total number of reviews received across all products. */
    @Schema(description = "Total reviews received", example = "1280")
    private Long totalReviews;

    // ═══════════════════════════════════════════════════════════════
    // TOP PRODUCTS
    // ═══════════════════════════════════════════════════════════════

    /** Ranked list of top-selling products. Empty list if no sales data is available. */
    @Schema(description = "Top selling products list")
    @Builder.Default
    private List<TopSellingProductResponse> topSellingProducts = List.of();

    /** Ranked list of top-rated products. Empty list if no rating data is available. */
    @Schema(description = "Top rated products list")
    @Builder.Default
    private List<TopRatedProduct> topRatedProducts = List.of();

    // ═══════════════════════════════════════════════════════════════
    // TEMPORAL DATA
    // ═══════════════════════════════════════════════════════════════

    /** Number of products added in the last 30 days. */
    @Schema(description = "Products added in last 30 days", example = "18")
    private Long productsAddedLast30Days;

    /** Number of products updated in the last 30 days. */
    @Schema(description = "Products updated in last 30 days", example = "65")
    private Long productsUpdatedLast30Days;

    /** Timestamp when this dashboard was generated. */
    @Schema(description = "Dashboard generation timestamp in ISO-8601 format",
            example = "2026-07-03T15:30:00")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime generatedAt;

    // ═══════════════════════════════════════════════════════════════
    // NESTED DTOS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Top rated product summary.
     *
     * <p>Contains product identification, rating metrics, and pricing for a top-rated product.
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Top rated product summary")
    public static class TopRatedProduct implements Serializable {

        private static final long serialVersionUID = 1L;

        /** Unique identifier of the product. */
        @Schema(description = "Product ID", example = "789")
        private Long productId;

        /** Display name of the product. */
        @Schema(description = "Product name", example = "Premium Wireless Headphones")
        private String productName;

        /** Stock-keeping unit code. */
        @Schema(description = "Product SKU", example = "WH-1000XM5")
        private String sku;

        /** Average customer rating for this product. */
        @Schema(description = "Average rating", example = "4.8")
        private Double averageRating;

        /** Total number of reviews for this product. */
        @Schema(description = "Total reviews", example = "450")
        private Long reviewCount;

        /** Current listed price of the product. */
        @Schema(description = "Current price in seller's local currency", example = "349.99")
        @MonetaryField
        private BigDecimal price;

        /** URL of the product's primary image. */
        @Schema(description = "Product image URL", example = "https://cdn.example.com/images/products/wh-1000xm5.jpg")
        private String imageUrl;
    }
}
