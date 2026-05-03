package com.eshop.app.service.analytics;

import com.eshop.app.dto.response.AdminDashboardResponse;
import com.eshop.app.service.OrderService;
import com.eshop.app.service.ProductService;
import com.eshop.app.service.StoreService;
import com.eshop.app.service.UserService;
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
    private final ProductService productService;
    private final StoreService storeService;
    private final OrderService orderService;
    private final Executor eshopVirtualThreadExecutor;

    public AdminDashboardResponse.OverviewStats getOverviewStats() {
        try {
            CompletableFuture<Long> totalUsers = CompletableFuture.supplyAsync(userService::getTotalUserCount, eshopVirtualThreadExecutor);
            CompletableFuture<Long> totalProducts = CompletableFuture.supplyAsync(productService::getTotalProductCount, eshopVirtualThreadExecutor);
            CompletableFuture<Long> totalStores = CompletableFuture.supplyAsync(storeService::getTotalStoreCount, eshopVirtualThreadExecutor);
            CompletableFuture<Long> totalOrders = CompletableFuture.supplyAsync(orderService::getTotalOrderCount, eshopVirtualThreadExecutor);
            CompletableFuture<Long> pendingOrders = CompletableFuture.supplyAsync(orderService::getPendingOrderCount, eshopVirtualThreadExecutor);
            CompletableFuture<Long> todayOrders = CompletableFuture.supplyAsync(orderService::getTodayOrderCount, eshopVirtualThreadExecutor);

            CompletableFuture.allOf(totalUsers, totalProducts, totalStores, totalOrders, pendingOrders, todayOrders).join();

            AdminDashboardResponse.OverviewStats overview = AdminDashboardResponse.OverviewStats.builder()
                    .totalUsers(safeGet(totalUsers, 0L))
                    .totalProducts(safeGet(totalProducts, 0L))
                    .totalStores(safeGet(totalStores, 0L))
                    .totalOrders(safeGet(totalOrders, 0L))
                    .pendingOrders(safeGet(pendingOrders, 0L))
                    .todayOrders(safeGet(todayOrders, 0L))
                    .totalRevenue(orderService.getTotalRevenue())
                    .monthlyRevenue(orderService.getMonthlyRevenue())
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
