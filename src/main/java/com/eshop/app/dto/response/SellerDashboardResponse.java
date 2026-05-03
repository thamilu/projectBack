package com.eshop.app.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SellerDashboardResponse {
    private StoreOverview storeOverview;
    private SalesMetrics salesMetrics;
    private OrderManagement orderManagement;
    private List<?> topProducts;
    private List<?> recentOrders;
    private String role;
    private Instant timestamp;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StoreOverview {
        private String storeName;
        private String storeStatus;
        private Long totalProducts;
        private Long activeProducts;
        private Long outOfStockProducts;
        private Double storeRating;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SalesMetrics {
        private BigDecimal todaySales;
        private BigDecimal weeklySales;
        private BigDecimal monthlySales;
        private BigDecimal totalSales;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderManagement {
        private Long newOrders;
        private Long processingOrders;
        private Long shippedOrders;
        private Long completedOrders;
    }
}
