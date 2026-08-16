package com.eshop.app.seller.api.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Response DTO representing a top-selling product entry in seller analytics.
 *
 * <p>Used by {@code GET /dashboard/seller/analytics/top-products} to return ranked product
 * performance data for the authenticated seller.
 */
@Schema(description = "Top-selling product entry with sales performance metrics")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TopProductResponse {

    @Schema(description = "Product ID", example = "1001")
    private Long productId;

    @Schema(description = "Product name", example = "Organic Basmati Rice 5kg")
    private String productName;

    @Schema(description = "Product SKU", example = "ORG-RICE-5KG")
    private String sku;

    @Schema(
            description = "Product thumbnail URL",
            example = "https://cdn.example.com/images/products/org-rice-5kg-thumb.jpg")
    private String thumbnailUrl;

    @Schema(description = "Category name", example = "Grains & Cereals")
    private String categoryName;

    @Schema(description = "Total units sold in the period", example = "342")
    private Long unitsSold;

    @MonetaryField
    @Schema(description = "Total revenue generated", example = "51300.00")
    private BigDecimal totalRevenue;

    @MonetaryField
    @Schema(description = "Average selling price", example = "150.00")
    private BigDecimal averagePrice;

    @Schema(description = "Sales rank within seller's catalog (1 = best seller)", example = "1")
    private Integer salesRank;

    @Schema(description = "Revenue contribution percentage", example = "23.5")
    private Double revenueContributionPercent;
}
