package com.eshop.app.seller.api.response;





import lombok.*;
import java.math.BigDecimal;

/**
 * Enterprise-grade DTO for aggregated seller metrics.
 * Used to fetch multiple statistics in a single database round-trip.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellerAggregationMetricsDTO {
    private BigDecimal todaySales;
    private BigDecimal weeklySales;
    private BigDecimal monthlySales;
    private BigDecimal totalSales;
    private Long newOrders;
    private Long processingOrders;
    private Long shippedOrders;
    private Long completedOrders;
}
