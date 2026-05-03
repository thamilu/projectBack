package com.eshop.app.service;

import com.eshop.app.dto.response.CustomerDashboardResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class CustomerDashboardService {

        private final UserService userService;
        private final com.eshop.app.service.OrderService orderService;
        private final com.eshop.app.service.ProductService productService;

    public CustomerDashboardResponse getDashboard(Long customerId) {
        CustomerDashboardResponse.AccountInfo account = CustomerDashboardResponse.AccountInfo.builder()
                .customerName(null)
                .email(null)
                .memberSince(userService.getMemberSinceByUserId(customerId) != null ? userService.getMemberSinceByUserId(customerId).toString() : null)
                .accountStatus("Active")
                .emailVerified(null)
                .totalOrders(orderService.getOrderCountByCustomerId(customerId))
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
                .totalSpent(orderService.getTotalSpentByCustomerId(customerId))
                .averageOrderValue(orderService.getAverageOrderValueByCustomerId(customerId))
                .favoriteCategory(productService.getFavoriteCategoryByCustomerId(customerId).orElse(null))
                .build();

        // Fetch Trending Products (Top Selling)
        java.util.List<com.eshop.app.dto.response.TopSellingProductResponse> trending = productService
                        .getTopSellingProducts(5);

        // Fetch Featured Products (using featured query)
        // Using PageRequest directly here might need an import, or use
        // Pageable.ofSize(5)
        com.eshop.app.dto.response.PageResponse<com.eshop.app.dto.response.ProductResponse> featuredPage = productService
                        .getFeaturedProducts(org.springframework.data.domain.PageRequest.of(0, 5));

        // Find Active Order (First order that is NOT DELIVERED or CANCELLED)

        java.util.List<java.util.Map<String, Object>> rawRecentOrders = orderService
                        .getRecentOrdersByCustomerId(customerId, 10);

        Object activeOrder = rawRecentOrders.stream()
                        .filter(order -> {
                                String status = (String) order.get("status");
                                return !"DELIVERED".equals(status) && !"CANCELLED".equals(status);
                        })
                        .findFirst()
                        .orElse(null);

        return CustomerDashboardResponse.builder()
                .accountInfo(account)
                        .recentOrders(rawRecentOrders)
                .cartInfo(cart)
                .wishlistInfo(wishlist)
                .recommendations(java.util.List.of())
                .orderStats(stats)
                .role("CUSTOMER")
                .timestamp(Instant.now())
                        .trendingProducts(trending)
                        .featuredProducts(featuredPage.getContent())
                        .activeOrder(activeOrder)
                .build();
    }
    
    /**
     * Find customer ID by username (for Keycloak integration)
     */
    public Long findCustomerIdByUsername(String username, String email, String firstName, String lastName,
                    Boolean emailVerified, String keycloakId, String phoneNumber) {
            // Prefer lookup by Keycloak ID
            java.util.Optional<Long> idOpt = userService.findUserIdByKeycloakId(keycloakId);
            if (idOpt.isPresent())
                    return idOpt.get();

        // Create local user from Keycloak ID for first-time login
        return userService.createUserFromKeycloak(keycloakId, firstName, lastName, phoneNumber);
}
}
