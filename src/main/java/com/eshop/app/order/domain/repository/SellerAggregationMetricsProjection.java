package com.eshop.app.order.domain.repository;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SellerAggregationMetricsProjection {
    private BigDecimal todaySales;
    private BigDecimal weeklySales;
    private BigDecimal monthlySales;
    private BigDecimal totalSales;
    private Long newOrders;
    private Long processingOrders;
    private Long shippedOrders;
    private Long completedOrders;
}
