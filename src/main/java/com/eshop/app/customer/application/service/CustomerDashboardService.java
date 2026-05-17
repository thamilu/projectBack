package com.eshop.app.customer.application.service;

import com.eshop.app.customer.api.response.CustomerDashboardResponse;
import com.eshop.app.user.application.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerDashboardService {

    private final UserService userService;
    private final com.eshop.app.order.application.port.in.OrderAnalyticsUseCase orderAnalyticsUseCase;
    private final com.eshop.app.catalog.application.port.in.ProductUseCase productService;

    public CustomerDashboardResponse getDashboard(Long customerId) {
        if (customerId == null || customerId == -1L) {
            return createEmptyDashboard();
        }

        try {
            CustomerDashboardResponse.AccountInfo account = CustomerDashboardResponse.AccountInfo.builder()
                    .customerName(null)
                    .email(null)
                    .memberSince(safeGetMemberSince(customerId))
                    .accountStatus("Active")
                    .emailVerified(null)
                    .totalOrders(safeGetOrderCount(customerId))
                    .build();

            CustomerDashboardResponse.CartInfo cart = CustomerDashboardResponse.CartInfo.builder()
                    .itemCount(0L)
                    .totalValue(java.math.BigDecimal.ZERO)
                    .build();

            CustomerDashboardResponse.WishlistInfo wishlist = CustomerDashboardResponse.WishlistInfo.builder()
                    .itemCount(0L)
                    .recentlyAdded(java.util.List.of())
                    .build();

            CustomerDashboardResponse.OrderStats stats = CustomerDashboardResponse.OrderStats.builder()
                    .totalSpent(safeGetTotalSpent(customerId))
                    .averageOrderValue(safeGetAverageOrderValue(customerId))
                    .favoriteCategory(safeGetFavoriteCategory(customerId))
                    .build();

            // Fetch Trending/Featured Products
            java.util.List<com.eshop.app.catalog.api.response.TopSellingProductResponse> trending = safeGetTrendingProducts();
            java.util.List<com.eshop.app.catalog.api.response.ProductResponse> featured = safeGetFeaturedProducts();

            java.util.List<java.util.Map<String, Object>> rawRecentOrders = safeGetRecentOrders(customerId);

            Object activeOrder = rawRecentOrders != null ? rawRecentOrders.stream()
                    .filter(order -> {
                        String status = (String) order.get("status");
                        return !"DELIVERED".equals(status) && !"CANCELLED".equals(status);
                    })
                    .findFirst()
                    .orElse(null) : null;

            return CustomerDashboardResponse.builder()
                    .accountInfo(account)
                    .recentOrders(rawRecentOrders != null ? rawRecentOrders : java.util.List.of())
                    .cartInfo(cart)
                    .wishlistInfo(wishlist)
                    .recommendations(java.util.List.of())
                    .orderStats(stats)
                    .role("CUSTOMER")
                    .timestamp(Instant.now())
                    .trendingProducts(trending)
                    .featuredProducts(featured)
                    .activeOrder(activeOrder)
                    .build();
        } catch (Exception e) {
            return createEmptyDashboard();
        }
    }

    private String safeGetMemberSince(Long customerId) {
        try {
            var date = userService.getMemberSinceByUserId(customerId);
            return date != null ? date.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private Long safeGetOrderCount(Long customerId) {
        try {
            return orderAnalyticsUseCase.getOrderCountByCustomerId(customerId);
        } catch (Exception e) {
            return 0L;
        }
    }

    private java.math.BigDecimal safeGetTotalSpent(Long customerId) {
        try {
            return orderAnalyticsUseCase.getTotalSpentByCustomerId(customerId);
        } catch (Exception e) {
            return java.math.BigDecimal.ZERO;
        }
    }

    private java.math.BigDecimal safeGetAverageOrderValue(Long customerId) {
        try {
            return orderAnalyticsUseCase.getAverageOrderValueByCustomerId(customerId);
        } catch (Exception e) {
            return java.math.BigDecimal.ZERO;
        }
    }

    private String safeGetFavoriteCategory(Long customerId) {
        try {
            return productService.getFavoriteCategoryByCustomerId(customerId).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private java.util.List<com.eshop.app.catalog.api.response.TopSellingProductResponse> safeGetTrendingProducts() {
        try {
            return productService.getTopSellingProducts(5);
        } catch (Exception e) {
            return java.util.List.of();
        }
    }

    private java.util.List<com.eshop.app.catalog.api.response.ProductResponse> safeGetFeaturedProducts() {
        try {
            var page = productService.getFeaturedProducts(org.springframework.data.domain.PageRequest.of(0, 5));
            return page != null && page.getContent() != null ? page.getContent() : java.util.List.of();
        } catch (Exception e) {
            return java.util.List.of();
        }
    }

    private java.util.List<java.util.Map<String, Object>> safeGetRecentOrders(Long customerId) {
        try {
            return orderAnalyticsUseCase.getRecentOrdersByCustomerId(customerId, 10);
        } catch (Exception e) {
            return java.util.List.of();
        }
    }

    private CustomerDashboardResponse createEmptyDashboard() {
        return CustomerDashboardResponse.builder()
                .accountInfo(new CustomerDashboardResponse.AccountInfo())
                .recentOrders(java.util.List.of())
                .cartInfo(new CustomerDashboardResponse.CartInfo(0L, java.math.BigDecimal.ZERO))
                .wishlistInfo(new CustomerDashboardResponse.WishlistInfo(0L, java.util.List.of()))
                .trendingProducts(java.util.List.of())
                .featuredProducts(java.util.List.of())
                .timestamp(Instant.now())
                .build();
    }

    public Long findCustomerIdByEmail(String email, String firstName, String lastName,
            Boolean emailVerified, String keycloakId, String phoneNumber) {
        if (keycloakId == null || keycloakId.isBlank()) return null;
        try {
            return userService.syncUserFromKeycloak(keycloakId, email, firstName, lastName, phoneNumber, emailVerified);
        } catch (Exception e) {
            throw e;
        }
    }
}
