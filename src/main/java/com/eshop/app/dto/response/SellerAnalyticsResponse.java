package com.eshop.app.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellerAnalyticsResponse {
    private List<SalesTrendData> salesTrend;
    private List<ProductPerformanceData> productPerformance;
    private CustomerDemographicsData customerDemographics;
    private Map<String, BigDecimal> revenueBreakdown;
    private Instant generatedAt;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SalesTrendData {
        private String period;
        private BigDecimal revenue;
        private Long orderCount;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductPerformanceData {
        private Long productId;
        private String productName;
        private Long views;
        private Long sales;
        private BigDecimal conversionRate;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomerDemographicsData {
        private Map<String, Long> byRegion;
        private Map<String, Long> byAge;
        private Long totalCustomers;
    }
}
