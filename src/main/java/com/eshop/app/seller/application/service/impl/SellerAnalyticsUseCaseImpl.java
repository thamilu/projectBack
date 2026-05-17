package com.eshop.app.seller.application.service.impl;

import com.eshop.app.catalog.domain.repository.ProductRepositoryEnhanced;
import com.eshop.app.order.domain.repository.AnalyticsOrderRepository;
import com.eshop.app.seller.api.response.SellerStatistics;
import com.eshop.app.seller.application.port.in.SellerAnalyticsUseCase;

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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SellerAnalyticsUseCaseImpl implements SellerAnalyticsUseCase {
    private static final Logger log = LoggerFactory.getLogger(SellerAnalyticsUseCaseImpl.class);

    private final AnalyticsOrderRepository analyticsOrderRepository;
    private final ProductRepositoryEnhanced productRepository;
    private final Executor dashboardExecutor;

    @Override
    @Cacheable(value = "sellerStatistics", key = "#sellerId", unless = "#result == null")
    public SellerStatistics getSellerStatistics(Long sellerId) {
        log.debug("Calculating seller statistics for seller ID: {}", sellerId);

        long startTime = System.currentTimeMillis();

        Map<String, Object> orderStats = analyticsOrderRepository.getSellerOrderStatistics(sellerId);

        Map<String, Object> productStats = productRepository.getProductStatisticsBySellerId(sellerId);

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
                .monthlyOrders(0L)
                .build();

        long executionTime = System.currentTimeMillis() - startTime;
        log.debug("Seller statistics calculated in {} ms", executionTime);

        return statistics;
    }

    @Override
    public CompletableFuture<SellerStatistics> getSellerStatisticsAsync(Long sellerId) {
        return CompletableFuture.supplyAsync(() -> getSellerStatistics(sellerId), dashboardExecutor);
    }

    @Override
    public java.util.List<Map<String, Object>> getTopSellingProducts(Long sellerId, int limit) {
        return productRepository.getTopSellingProductsBySellerId(sellerId,
                org.springframework.data.domain.PageRequest.of(0, limit));
    }

    @Override
    public CompletableFuture<java.util.List<Map<String, Object>>> getTopSellingProductsAsync(Long sellerId, int limit) {
        return CompletableFuture.supplyAsync(() -> getTopSellingProducts(sellerId, limit), dashboardExecutor);
    }

    @Override
    public java.util.List<Map<String, Object>> getSalesTrend(
            Long sellerId,
            LocalDateTime startDate,
            LocalDateTime endDate) {
        return java.util.List.of();
    }

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

    private Double getDouble(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null)
            return 0.0;
        if (value instanceof Double)
            return (Double) value;
        if (value instanceof Number)
            return ((Number) value).doubleValue();
        return 0.0;
    }
}
