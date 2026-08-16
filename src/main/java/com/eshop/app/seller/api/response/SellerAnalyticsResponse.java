package com.eshop.app.seller.api.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response DTO representing seller analytics reports.
 * Aggregates order sales trends, top product performance list, and customer demographics charts.
 *
 * @since 1.0
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Seller analytics dashboard response details")
public class SellerAnalyticsResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Periodic sales trends. */
    @Schema(description = "List of periodic sales trend points")
    private List<SalesTrendData> salesTrend;

    /** List of product-specific performance metrics. */
    @Schema(description = "List of product views and conversion data")
    private List<ProductPerformanceData> productPerformance;

    /** Customer demographics aggregation values. */
    @Schema(description = "Customer demographics insights grouped by region and age range")
    private CustomerDemographicsData customerDemographics;

    /**
     * Map grouping sales revenue by dynamic time periods or channels.
     * Uses MoneySerializer under the hood to ensure consistent decimal formatting.
     */
    @JsonSerialize(contentUsing = MoneySerializer.class)
    @Schema(
            description = "Revenue breakdowns by category or channel. Values are formatted to 2 decimal places. " +
                    "Keys represent categories or channels (e.g. 'ONLINE', 'OFFLINE', 'MARKETPLACE').",
            example = "{\"ONLINE\": \"12500.50\", \"OFFLINE\": \"8400.00\"}"
    )
    private Map<String, BigDecimal> revenueBreakdown;

    /** Timestamp when this analytics data was aggregated. */
    @Schema(description = "ISO-8601 formatted timestamp when the metrics were computed.", example = "2026-07-04T05:00:00Z")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "UTC")
    private Instant generatedAt;

    /**
     * Data point representing sales trends over a specific period.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Sales trend data point")
    public static class SalesTrendData implements Serializable {
        private static final long serialVersionUID = 1L;

        @Schema(description = "Time period descriptor", example = "2026-07")
        private String period;

        @MonetaryField
        @Schema(description = "Total revenue generated in the period, serialized to 2 decimal places.", example = "2500.00")
        private BigDecimal revenue;

        @Schema(description = "Total order count in the period", example = "50")
        private Long orderCount;
    }

    /**
     * Data point representing views and conversion metrics for a product.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Product performance metrics details")
    public static class ProductPerformanceData implements Serializable {
        private static final long serialVersionUID = 1L;

        @Schema(description = "Product record ID", example = "101")
        private Long productId;

        @Schema(description = "Product display name", example = "Organic Basmati Rice 5kg")
        private String productName;

        @Schema(description = "Total page views count", example = "1500")
        private Long views;

        @Schema(description = "Total units sold", example = "225")
        private Long sales;

        @Schema(description = "Sales conversion rate expressed as a decimal ratio (0.15 = 15%).", example = "0.15")
        private BigDecimal conversionRate;
    }

    /**
     * Data aggregation representing customer demographic breakdowns.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Customer demographics groupings")
    public static class CustomerDemographicsData implements Serializable {
        private static final long serialVersionUID = 1L;

        @Schema(
                description = "Customer count grouped by geographical regions/states.",
                example = "{\"Karnataka\": 450, \"Tamil Nadu\": 300}"
        )
        private Map<String, Long> byRegion;

        @Schema(
                description = "Customer count grouped by age range categories.",
                example = "{\"18-24\": 120, \"25-34\": 500}"
        )
        private Map<String, Long> byAge;

        @Schema(description = "Total count of unique customers", example = "1250")
        private Long totalCustomers;
    }
}
