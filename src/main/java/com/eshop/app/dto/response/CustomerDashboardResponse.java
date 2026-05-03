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
public class CustomerDashboardResponse {
    private AccountInfo accountInfo;
    private List<?> recentOrders;
    private CartInfo cartInfo;
    private WishlistInfo wishlistInfo;
    private List<?> recommendations;
    private OrderStats orderStats;
    private String role;
    private Instant timestamp;
    private List<TopSellingProductResponse> trendingProducts;
    private List<ProductResponse> featuredProducts;
    private Object activeOrder;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AccountInfo {
        private String customerName;
        private String email;
        private String memberSince;
        private String accountStatus;
        private Boolean emailVerified;
        private Long totalOrders;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CartInfo {
        private Long itemCount;
        private BigDecimal totalValue;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WishlistInfo {
        private Long itemCount;
        private List<?> recentlyAdded;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderStats {
        private BigDecimal totalSpent;
        private BigDecimal averageOrderValue;
        private String favoriteCategory;
    }
}
