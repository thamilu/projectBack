package com.eshop.app.seller.application.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.eshop.app.catalog.domain.repository.ProductRepositoryEnhanced;
import com.eshop.app.order.domain.repository.AnalyticsOrderRepository;
import com.eshop.app.seller.api.response.SellerStatistics;
import com.eshop.app.seller.application.service.impl.SellerAnalyticsUseCaseImpl;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SellerAnalyticsUseCaseTest {

    @Mock private AnalyticsOrderRepository analyticsOrderRepository;

    @Mock private ProductRepositoryEnhanced productRepository;

    private SellerAnalyticsUseCaseImpl analyticsService;

    @BeforeEach
    void setUp() {
        analyticsService =
                new SellerAnalyticsUseCaseImpl(
                        analyticsOrderRepository,
                        productRepository,
                        Runnable::run
                );
    }

    @Test
    void getSellerStatistics_withValidId_shouldCalculateMetrics() {
        Long sellerId = 1L;

        Map<String, Object> orderStats = new HashMap<>();
        orderStats.put("totalOrders", 100L);
        orderStats.put("totalRevenue", new BigDecimal("50000.00"));
        orderStats.put("pendingOrders", 5L);
        orderStats.put("completedOrders", 90L);
        orderStats.put("cancelledOrders", 5L);
        orderStats.put("totalCustomers", 80L);

        Map<String, Object> productStats = new HashMap<>();
        productStats.put("totalProducts", 50L);
        productStats.put("activeProducts", 45L);
        productStats.put("averageRating", 4.8);

        when(analyticsOrderRepository.getSellerOrderStatistics(sellerId)).thenReturn(orderStats);
        when(productRepository.getProductStatisticsBySellerId(sellerId)).thenReturn(productStats);
        when(analyticsOrderRepository.getMonthlyRevenueBySellerId(
                        eq(sellerId), any(LocalDateTime.class)))
                .thenReturn(new BigDecimal("12500.00"));

        SellerStatistics stats = analyticsService.getSellerStatistics(sellerId);

        assertNotNull(stats);
        assertEquals(100L, stats.getTotalOrders());
        assertEquals(new BigDecimal("50000.00"), stats.getTotalRevenue());
        assertEquals(5L, stats.getPendingOrders());
        assertEquals(90L, stats.getCompletedOrders());
        assertEquals(5L, stats.getCancelledOrders());
        assertEquals(80L, stats.getTotalCustomers());
        assertEquals(50L, stats.getTotalProducts());
        assertEquals(45L, stats.getActiveProducts());
        assertEquals(4.8, stats.getAverageRating());
        assertEquals(new BigDecimal("12500.00"), stats.getMonthlyRevenue());
    }

    @Test
    void getSellerStatisticsAsync_shouldExecuteAsyncAndReturnFuture() throws Exception {
        Long sellerId = 1L;

        Map<String, Object> orderStats = new HashMap<>();
        orderStats.put("totalOrders", 50L);
        orderStats.put("totalRevenue", new BigDecimal("2000.00"));
        orderStats.put("pendingOrders", 2L);
        orderStats.put("completedOrders", 48L);
        orderStats.put("cancelledOrders", 0L);
        orderStats.put("totalCustomers", 40L);

        Map<String, Object> productStats = new HashMap<>();
        productStats.put("totalProducts", 10L);
        productStats.put("activeProducts", 10L);
        productStats.put("averageRating", 4.5);

        when(analyticsOrderRepository.getSellerOrderStatistics(sellerId)).thenReturn(orderStats);
        when(productRepository.getProductStatisticsBySellerId(sellerId)).thenReturn(productStats);
        when(analyticsOrderRepository.getMonthlyRevenueBySellerId(
                        eq(sellerId), any(LocalDateTime.class)))
                .thenReturn(new BigDecimal("500.00"));

        CompletableFuture<SellerStatistics> future =
                analyticsService.getSellerStatisticsAsync(sellerId);
        SellerStatistics stats = future.get();

        assertNotNull(stats);
        assertEquals(50L, stats.getTotalOrders());
        assertEquals(new BigDecimal("2000.00"), stats.getTotalRevenue());
    }

    @Test
    void getTopSellingProducts_withValidParams_shouldDelegateToRepository() {
        Long sellerId = 1L;
        int limit = 10;
        List<Map<String, Object>> mockList = List.of(Map.of("productId", 101L, "productName", "Sample Product"));

        when(productRepository.getTopSellingProductsBySellerId(eq(sellerId), any())).thenReturn(mockList);

        List<Map<String, Object>> result = analyticsService.getTopSellingProducts(sellerId, limit);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Sample Product", result.get(0).get("productName"));
    }

    @Test
    void getSalesTrend_shouldReturnSafeDefault() {
        Long sellerId = 1L;
        LocalDateTime start = LocalDateTime.now().minusDays(30);
        LocalDateTime end = LocalDateTime.now();

        List<Map<String, Object>> result = analyticsService.getSalesTrend(sellerId, start, end);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
