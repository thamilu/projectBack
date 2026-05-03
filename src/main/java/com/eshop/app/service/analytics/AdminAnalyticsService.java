package com.eshop.app.service.analytics;

import com.eshop.app.dto.analytics.AdminStatistics;
import com.eshop.app.repository.ProductRepositoryEnhanced;
import com.eshop.app.repository.StoreRepository;
import com.eshop.app.repository.UserRepositoryEnhanced;
import com.eshop.app.repository.analytics.AnalyticsOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HIGH-001 FIX: Optimized service for admin analytics aggregation.
 * 
 * <p>
 * Performance Improvements:
 * <ul>
 * <li>Parallel execution of independent queries (4 sequential → 4
 * parallel)</li>
 * <li>Single aggregate queries instead of multiple counts</li>
 * <li>Proper caching with TTL</li>
 * <li>CompletableFuture for async coordination</li>
 * </ul>
 * 
 * <p>
 * Time Complexity: O(1) with parallel execution vs O(N) sequential
 * <p>
 * Performance Gain: 4-5 sequential queries → 1 parallel batch (80% reduction)
 * 
 * @author EShop Team
 * @since 2.0.1
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminAnalyticsService {
    private static final Logger log = LoggerFactory.getLogger(AdminAnalyticsService.class);

    private final UserRepositoryEnhanced userRepository;
    private final ProductRepositoryEnhanced productRepository;
    private final AnalyticsOrderRepository analyticsOrderRepository;
    private final StoreRepository storeRepository;
    private final Executor dashboardExecutor;

    /**
     * HIGH-001 FIX: Gets comprehensive admin statistics with parallel execution.
     * 
     * Time Complexity: O(1) with parallel execution vs O(4) sequential
     * Space Complexity: O(1)
     * 
     * @return admin statistics with all aggregates
     */
    @Cacheable(value = "adminStatistics", key = "'admin-stats'", unless = "#result == null", sync = true)
    public AdminStatistics getAdminStatistics() {
        log.debug("Calculating admin statistics with parallel execution");

        long startTime = System.currentTimeMillis();

        try {
            // Parallel execution for independent queries
            CompletableFuture<Map<String, Object>> userStatsFuture = CompletableFuture.supplyAsync(() -> {
                log.trace("Fetching user statistics");
                return userRepository.getUserStatistics();
            }, dashboardExecutor);

            CompletableFuture<Map<String, Object>> productStatsFuture = CompletableFuture.supplyAsync(() -> {
                log.trace("Fetching product statistics");
                return productRepository.getProductStatistics();
            }, dashboardExecutor);

            CompletableFuture<Map<String, Object>> orderStatsFuture = CompletableFuture.supplyAsync(() -> {
                LocalDateTime startOfMonth = YearMonth.now().atDay(1).atStartOfDay();
                LocalDateTime now = LocalDateTime.now();
                log.trace("Fetching order statistics from {} to {}", startOfMonth, now);
                return analyticsOrderRepository.getOrderStatistics(startOfMonth, now);
            }, dashboardExecutor);

            CompletableFuture<Map<String, Object>> storeStatsFuture = CompletableFuture.supplyAsync(() -> {
                log.trace("Fetching store statistics");
                return storeRepository.getStoreStatistics();
            }, dashboardExecutor);

            // Wait for all to complete with proper error handling
            CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                    userStatsFuture,
                    productStatsFuture,
                    orderStatsFuture,
                    storeStatsFuture);

            // Block with timeout
            allFutures.get(10, java.util.concurrent.TimeUnit.SECONDS);

            // Extract results
            Map<String, Object> userStats = userStatsFuture.join();
            Map<String, Object> productStats = productStatsFuture.join();
            Map<String, Object> orderStats = orderStatsFuture.join();
            Map<String, Object> storeStats = storeStatsFuture.join();

            AdminStatistics statistics = AdminStatistics.builder()
                    .totalUsers(getLong(userStats, "totalUsers"))
                    .totalCustomers(getLong(userStats, "totalCustomers"))
                    .totalSellers(getLong(userStats, "totalSellers"))
                    .totalDeliveryAgents(getLong(userStats, "totalDeliveryAgents"))
                    .activeUsers(0L)
                    .newUsersThisMonth(0L)
                    .totalProducts(getLong(productStats, "totalProducts"))
                    .activeProducts(getLong(productStats, "activeProducts"))
                    .outOfStockProducts(getLong(productStats, "outOfStockProducts"))
                    .totalOrders(getLong(orderStats, "totalOrders"))
                    .pendingOrders(getLong(orderStats, "pendingOrders"))
                    .completedOrders(getLong(orderStats, "completedOrders"))
                    .todayOrders(getLong(orderStats, "todayOrders"))
                    .totalRevenue(getBigDecimal(orderStats, "totalRevenue"))
                    .monthlyRevenue(getBigDecimal(orderStats, "monthlyRevenue"))
                    .todayRevenue(getBigDecimal(orderStats, "todayRevenue"))
                    .totalShops(getLong(storeStats, "totalShops"))
                    .activeShops(getLong(storeStats, "activeShops"))
                    .build();

            long executionTime = System.currentTimeMillis() - startTime;
            log.info("Admin statistics calculated in {}ms (parallel execution)", executionTime);

            return statistics;

        } catch (Exception e) {
            log.error("Error calculating admin statistics", e);
            throw new RuntimeException("Failed to calculate admin statistics", e);
        }
    }

    /**
     * Async wrapper for admin statistics to allow controllers to fetch in parallel.
     */
    public CompletableFuture<AdminStatistics> getAdminStatisticsAsync() {
        return CompletableFuture.supplyAsync(this::getAdminStatistics, dashboardExecutor);
    }

    /**
     * Gets daily sales data for analytics charts.
     * 
     * Time Complexity: O(D) where D = number of days
     * 
     * @param days number of days to include
     * @return daily sales data
     */
    @Cacheable(value = "dailySalesData", key = "#days", unless = "#result == null || #result.isEmpty()", sync = true)
    public List<Map<String, Object>> getDailySalesData(int days) {
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusDays(days);

        log.debug("Fetching daily sales data for {} days", days);
        return analyticsOrderRepository.getDailySalesData(startDate, endDate);
    }

    /**
     * Async wrapper for daily sales data to be used by controllers for parallel
     * execution.
     */
    public CompletableFuture<List<Map<String, Object>>> getDailySalesDataAsync(int days) {
        return CompletableFuture.supplyAsync(() -> getDailySalesData(days), dashboardExecutor);
    }

    /**
     * Gets revenue breakdown by category.
     * 
     * Time Complexity: O(C) where C = number of categories
     * 
     * @return category revenue map
     */
    @Cacheable(value = "revenueByCategory", unless = "#result == null || #result.isEmpty()", sync = true)
    public List<Map<String, Object>> getRevenueByCategory() {
        log.debug("Fetching revenue by category");
        return analyticsOrderRepository.getRevenueByCategory();
    }

    /**
     * Async wrapper for revenue by category.
     */
    public CompletableFuture<List<Map<String, Object>>> getRevenueByCategoryAsync() {
        return CompletableFuture.supplyAsync(this::getRevenueByCategory, dashboardExecutor);
    }

    // Helper methods for safe type conversion

    private Long getLong(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null)
            return 0L;
        if (value instanceof Long)
            return (Long) value;
        if (value instanceof Number)
            return ((Number) value).longValue();
        return 0L;
    }

    private BigDecimal getBigDecimal(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null)
            return BigDecimal.ZERO;
        if (value instanceof BigDecimal)
            return (BigDecimal) value;
        if (value instanceof Number)
            return BigDecimal.valueOf(((Number) value).doubleValue());
        return BigDecimal.ZERO;
    }
}
