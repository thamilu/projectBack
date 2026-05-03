package com.eshop.app.service.analytics;

import com.eshop.app.dto.analytics.SellerStatistics;
import com.eshop.app.repository.ProductRepositoryEnhanced;
import com.eshop.app.repository.analytics.AnalyticsOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service for aggregating seller statistics.
 * Implements Single Responsibility Principle - only handles seller analytics.
 * Optimized with single-query aggregations to prevent N+1 queries.
 * 
 * Time Complexity: O(1) - single aggregation query vs O(n) individual queries
 * 
 * @author EShop Team
 * @since 2.0
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)


public class SellerAnalyticsService {
    private static final Logger log = LoggerFactory.getLogger(SellerAnalyticsService.class);
    
    private final AnalyticsOrderRepository analyticsOrderRepository;
    private final ProductRepositoryEnhanced productRepository;
    private final Executor dashboardExecutor;

    /**
     * Calculates comprehensive seller statistics in a single database call.
     * Cached for 5 minutes to reduce database load.
     * 
     * Previous implementation: 13 separate queries = O(13)
     * Current implementation: 3 queries = O(3)
     * Performance improvement: ~76% reduction in database calls
     * 
     * @param sellerId the seller ID
     * @return aggregated seller statistics
     */
    @Cacheable(value = "sellerStatistics", key = "#sellerId", unless = "#result == null")
    public SellerStatistics getSellerStatistics(Long sellerId) {
        log.debug("Calculating seller statistics for seller ID: {}", sellerId);
        
        long startTime = System.currentTimeMillis();
        
        // Query 1: Order statistics (single aggregation query)
        Map<String, Object> orderStats = analyticsOrderRepository.getSellerOrderStatistics(sellerId);
        
        // Query 2: Product statistics (single aggregation query)
        Map<String, Object> productStats = productRepository.getProductStatisticsBySellerId(sellerId);
        
        // Query 3: Monthly revenue
        LocalDateTime startOfMonth = YearMonth.now().atDay(1).atStartOfDay();
        BigDecimal monthlyRevenue = analyticsOrderRepository.getMonthlyRevenueBySellerId(sellerId, startOfMonth);
        
        SellerStatistics statistics = SellerStatistics.builder()
                .totalOrders(getLong(orderStats, "totalOrders"))
                .totalRevenue(getBigDecimal(orderStats, "totalRevenue"))
                .pendingOrders(getLong(orderStats, "pendingOrders"))
                .completedOrders(getLong(orderStats, "completedOrders"))
                .cancelledOrders(getLong(orderStats, "cancelledOrders"))
                .totalCustomers(getLong(orderStats, "totalCustomers"))
                .totalProducts(getLong(productStats, "totalProducts"))
                .activeProducts(getLong(productStats, "activeProducts"))
                .averageRating(getDouble(productStats, "averageRating"))
                .monthlyRevenue(monthlyRevenue)
                .monthlyOrders(0L) // Can be calculated if needed
                .build();
        
        long executionTime = System.currentTimeMillis() - startTime;
        log.debug("Seller statistics calculated in {} ms", executionTime);
        
        return statistics;
    }

    /**
     * Async wrapper for seller statistics.
     */
    public CompletableFuture<SellerStatistics> getSellerStatisticsAsync(Long sellerId) {
        return CompletableFuture.supplyAsync(() -> getSellerStatistics(sellerId), dashboardExecutor);
    }
    
    /**
     * Gets top selling products for a seller.
     * 
     * @param sellerId seller ID
     * @param limit max results
     * @return list of top products
     */
    public java.util.List<Map<String, Object>> getTopSellingProducts(Long sellerId, int limit) {
        return productRepository.getTopSellingProductsBySellerId(sellerId, 
                org.springframework.data.domain.PageRequest.of(0, limit));
    }

    /**
     * Async wrapper for top selling products.
     */
    public CompletableFuture<java.util.List<Map<String, Object>>> getTopSellingProductsAsync(Long sellerId, int limit) {
        return CompletableFuture.supplyAsync(() -> getTopSellingProducts(sellerId, limit), dashboardExecutor);
    }
    
    /**
     * Gets sales trend data for charts.
     * 
     * @param sellerId seller ID
     * @param startDate start date
     * @param endDate end date
     * @return daily sales trend
     */
    public java.util.List<Map<String, Object>> getSalesTrend(
            Long sellerId, 
            LocalDateTime startDate, 
            LocalDateTime endDate) {
        
        // Implementation would call repository method
        return java.util.List.of(); // Placeholder
    }
    
    // Helper methods for safe type conversion
    private Long getLong(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return 0L;
        if (value instanceof Long) return (Long) value;
        if (value instanceof Number) return ((Number) value).longValue();
        return 0L;
    }
    
    private BigDecimal getBigDecimal(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal) return (BigDecimal) value;
        if (value instanceof Number) return BigDecimal.valueOf(((Number) value).doubleValue());
        return BigDecimal.ZERO;
    }
    
    private Double getDouble(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return 0.0;
        if (value instanceof Double) return (Double) value;
        if (value instanceof Number) return ((Number) value).doubleValue();
        return 0.0;
    }
}
