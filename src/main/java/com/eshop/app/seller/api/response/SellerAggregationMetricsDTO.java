package com.eshop.app.seller.api.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response DTO representing aggregated seller metrics.
 *
 * <p><strong>Purpose:</strong> Populated from a single optimized database aggregation query
 * to minimize round-trips. Primarily used inside operational seller dashboard services.
 *
 * <p><strong>Monetary Serialization:</strong> All sales fields use the project's standard monetary
 * serialization (2 decimal places string rounding format).
 *
 * <p><strong>Null Boundary Handling:</strong> Order count values default to zero (0L) to prevent
 * null pointer exceptions or blank displays on the frontend.
 *
 * @since 1.0
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Aggregated seller sales and order performance metrics")
public class SellerAggregationMetricsDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Today's sales amount. */
    @MonetaryField
    @Schema(description = "Total sales amount generated today, formatted to 2 decimal places.", example = "12500.50")
    private BigDecimal todaySales;

    /** Weekly sales amount. */
    @MonetaryField
    @Schema(description = "Total sales amount generated in the last 7 days, formatted to 2 decimal places.", example = "84000.00")
    private BigDecimal weeklySales;

    /** Monthly sales amount. */
    @MonetaryField
    @Schema(description = "Total sales amount generated in the last 30 days, formatted to 2 decimal places.", example = "350000.00")
    private BigDecimal monthlySales;

    /** Lifetime total sales amount. */
    @MonetaryField
    @Schema(description = "Lifetime total sales amount generated, formatted to 2 decimal places.", example = "1500000.00")
    private BigDecimal totalSales;

    /** Number of new orders. */
    @Builder.Default
    @Schema(description = "Number of new orders requiring validation or processing.", example = "12")
    private Long newOrders = 0L;

    /** Number of processing orders. */
    @Builder.Default
    @Schema(description = "Number of orders currently in processing or assembly.", example = "8")
    private Long processingOrders = 0L;

    /** Number of shipped orders. */
    @Builder.Default
    @Schema(description = "Number of orders shipped and in transit to the customer.", example = "15")
    private Long shippedOrders = 0L;

    /** Number of completed orders. */
    @Builder.Default
    @Schema(description = "Total number of successfully completed and delivered orders.", example = "240")
    private Long completedOrders = 0L;
}
