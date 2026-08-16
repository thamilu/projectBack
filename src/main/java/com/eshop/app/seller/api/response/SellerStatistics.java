package com.eshop.app.seller.api.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Response DTO for aggregated seller statistics.
 *
 * <p>Contains order counts, revenue figures, product metrics, and customer data
 * for a single seller. Designed to be populated from a single aggregation query.
 *
 * @author EShop Team
 * @since 2.0
 */
@Schema(description = "Aggregated seller statistics including orders, revenue, products, and customer metrics")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SellerStatistics implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Total number of orders received by the seller. */
    @Schema(description = "Total number of orders received", example = "1250")
    private Long totalOrders;

    /** Total revenue earned by the seller. Serialized as a formatted monetary string. */
    @Schema(description = "Total revenue earned", example = "125000.00")
    @MonetaryField
    private BigDecimal totalRevenue;

    /** Number of orders currently in pending state. */
    @Schema(description = "Number of orders in pending state", example = "45")
    private Long pendingOrders;

    /** Number of orders that have been completed successfully. */
    @Schema(description = "Number of completed orders", example = "1100")
    private Long completedOrders;

    /** Number of orders that have been cancelled. */
    @Schema(description = "Number of cancelled orders", example = "105")
    private Long cancelledOrders;

    /** Total number of products listed by the seller. */
    @Schema(description = "Total number of listed products", example = "320")
    private Long totalProducts;

    /** Number of currently active (visible) products. */
    @Schema(description = "Number of active products", example = "280")
    private Long activeProducts;

    /** Average customer rating for the seller. */
    @Schema(description = "Average customer rating", example = "4.7")
    private Double averageRating;

    /** Total number of unique customers who have ordered from the seller. */
    @Schema(description = "Total number of unique customers", example = "890")
    private Long totalCustomers;

    /** Revenue earned in the current month. Serialized as a formatted monetary string. */
    @Schema(description = "Revenue earned in the current month", example = "15000.00")
    @MonetaryField
    private BigDecimal monthlyRevenue;

    /** Number of orders received in the current month. */
    @Schema(description = "Number of orders received in the current month", example = "120")
    private Long monthlyOrders;
}
