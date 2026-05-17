package com.eshop.app.admin.application.service;

import com.eshop.app.admin.api.response.AdminDashboardResponse;
import com.eshop.app.catalog.application.port.in.ProductUseCase;
import com.eshop.app.order.application.port.in.OrderAnalyticsUseCase;
import com.eshop.app.store.application.port.in.StoreUseCase;
import com.eshop.app.user.application.service.UserService;




import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Service
@RequiredArgsConstructor
public class AdminAggregationService {
    private static final Logger log = LoggerFactory.getLogger(AdminAggregationService.class);

    private final UserService userService;
    private final ProductUseCase productService;
    private final StoreUseCase storeService;
    private final OrderAnalyticsUseCase orderAnalyticsUseCase;
    private final Executor eshopVirtualThreadExecutor;

    public AdminDashboardResponse.OverviewStats getOverviewStats() {
        try {
            CompletableFuture<Long> totalUsers = CompletableFuture.supplyAsync(userService::getTotalUserCount, eshopVirtualThreadExecutor);
            CompletableFuture<Long> totalProducts = CompletableFuture.supplyAsync(productService::getTotalProductCount, eshopVirtualThreadExecutor);
            CompletableFuture<Long> totalStores = CompletableFuture.supplyAsync(storeService::getTotalStoreCount, eshopVirtualThreadExecutor);
            CompletableFuture<Long> totalOrders = CompletableFuture.supplyAsync(orderAnalyticsUseCase::getTotalOrderCount, eshopVirtualThreadExecutor);
            CompletableFuture<Long> pendingOrders = CompletableFuture.supplyAsync(orderAnalyticsUseCase::getPendingOrderCount, eshopVirtualThreadExecutor);
            CompletableFuture<Long> todayOrders = CompletableFuture.supplyAsync(orderAnalyticsUseCase::getTodayOrderCount, eshopVirtualThreadExecutor);

            CompletableFuture.allOf(totalUsers, totalProducts, totalStores, totalOrders, pendingOrders, todayOrders).join();

            AdminDashboardResponse.OverviewStats overview = AdminDashboardResponse.OverviewStats.builder()
                    .totalUsers(safeGet(totalUsers, 0L))
                    .totalProducts(safeGet(totalProducts, 0L))
                    .totalStores(safeGet(totalStores, 0L))
                    .totalOrders(safeGet(totalOrders, 0L))
                    .pendingOrders(safeGet(pendingOrders, 0L))
                    .todayOrders(safeGet(todayOrders, 0L))
                    .totalRevenue(orderAnalyticsUseCase.getTotalRevenue())
                    .monthlyRevenue(orderAnalyticsUseCase.getMonthlyRevenue())
                    .build();

            return overview;
        } catch (Exception e) {
            log.error("Failed to build overview stats: {}", e.getMessage(), e);
            return AdminDashboardResponse.OverviewStats.builder().build();
        }
    }

    public AdminDashboardResponse.UserStats getUserStats() {
        try {
            return AdminDashboardResponse.UserStats.builder()
                    .customers(userService.getCustomerCount())
                    .sellers(userService.getSellerCount())
                    .deliveryAgents(userService.getDeliveryAgentCount())
                    .activeUsers(userService.getActiveUserCount())
                    .newUsersThisMonth(userService.getNewUsersThisMonth())
                    .build();
        } catch (Exception e) {
            log.error("Failed to build user stats: {}", e.getMessage(), e);
            return AdminDashboardResponse.UserStats.builder().build();
        }
    }

    private <T> T safeGet(CompletableFuture<T> f, T fallback) {
        try { return f.join(); } catch (Exception e) { return fallback; }
    }
}
