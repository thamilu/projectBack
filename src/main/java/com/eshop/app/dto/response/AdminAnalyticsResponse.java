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
public class AdminAnalyticsResponse {
    private List<DailySalesData> dailySales;
    private List<MonthlySalesData> monthlySales;
    private List<TopProductData> topSellingProducts;
    private UserGrowthData userGrowth;
    private Map<String, BigDecimal> revenueByCategory;
    private Instant generatedAt;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailySalesData {
        private String date;
        private BigDecimal revenue;
        private Long orderCount;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlySalesData {
        private String month;
        private BigDecimal revenue;
        private Long orderCount;
        private BigDecimal growth;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopProductData {
        private Long productId;
        private String productName;
        private Long soldCount;
        private BigDecimal revenue;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserGrowthData {
        private Long totalUsers;
        private Long newUsersThisMonth;
        private Long newUsersToday;
        private Double growthPercentage;
    }
}
