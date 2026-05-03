package com.eshop.app.service.analytics;

import com.eshop.app.dto.response.SellerDashboardResponse;
import com.eshop.app.service.OrderService;
import com.eshop.app.service.ProductService;
import com.eshop.app.service.StoreService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Service
@RequiredArgsConstructor
public class SellerAggregationService {
    private static final Logger log = LoggerFactory.getLogger(SellerAggregationService.class);

    private final ProductService productService;
    private final OrderService orderService;
    private final StoreService storeService;
    private final Executor dashboardExecutor;

    public SellerDashboardResponse.StoreOverview buildStoreOverview(Long sellerId) {
        try {
            return SellerDashboardResponse.StoreOverview.builder()
                    .storeName(storeService.getStoreNameBySellerId(sellerId))
                    .storeStatus("Active")
                    .totalProducts(productService.getProductCountBySellerId(sellerId))
                    .activeProducts(productService.getActiveProductCountBySellerId(sellerId))
                    .outOfStockProducts(productService.getOutOfStockCountBySellerId(sellerId))
                    .storeRating(storeService.getStoreRatingBySellerId(sellerId))
                    .build();
        } catch (Exception e) {
            log.error("Failed to build store overview for {}: {}", sellerId, e.getMessage(), e);
            return SellerDashboardResponse.StoreOverview.builder().build();
        }
    }

    public SellerDashboardResponse.SalesMetrics buildSalesMetrics(Long sellerId, com.eshop.app.dto.response.SellerAggregationMetricsDTO metrics) {
        try {
            if (metrics == null) metrics = orderService.getSellerAggregationMetrics(sellerId);
            return SellerDashboardResponse.SalesMetrics.builder()
                    .todaySales(metrics.getTodaySales())
                    .weeklySales(metrics.getWeeklySales())
                    .monthlySales(metrics.getMonthlySales())
                    .totalSales(metrics.getTotalSales())
                    .build();
        } catch (Exception e) {
            log.error("Failed to build sales metrics for {}: {}", sellerId, e.getMessage(), e);
            return SellerDashboardResponse.SalesMetrics.builder().build();
        }
    }

    public SellerDashboardResponse.OrderManagement buildOrderManagement(Long sellerId, com.eshop.app.dto.response.SellerAggregationMetricsDTO metrics) {
        try {
            if (metrics == null) metrics = orderService.getSellerAggregationMetrics(sellerId);
            return SellerDashboardResponse.OrderManagement.builder()
                    .newOrders(metrics.getNewOrders())
                    .processingOrders(metrics.getProcessingOrders())
                    .shippedOrders(metrics.getShippedOrders())
                    .completedOrders(metrics.getCompletedOrders())
                    .build();
        } catch (Exception e) {
            log.error("Failed to build order management for {}: {}", sellerId, e.getMessage(), e);
            return SellerDashboardResponse.OrderManagement.builder().build();
        }
    }

    public CompletableFuture<SellerDashboardResponse.StoreOverview> buildStoreOverviewAsync(Long sellerId) {
        return CompletableFuture.supplyAsync(() -> buildStoreOverview(sellerId), dashboardExecutor);
    }

    public CompletableFuture<SellerDashboardResponse.SalesMetrics> buildSalesMetricsAsync(Long sellerId, com.eshop.app.dto.response.SellerAggregationMetricsDTO metrics) {
        return CompletableFuture.supplyAsync(() -> buildSalesMetrics(sellerId, metrics), dashboardExecutor);
    }

    public CompletableFuture<SellerDashboardResponse.OrderManagement> buildOrderManagementAsync(Long sellerId, com.eshop.app.dto.response.SellerAggregationMetricsDTO metrics) {
        return CompletableFuture.supplyAsync(() -> buildOrderManagement(sellerId, metrics), dashboardExecutor);
    }
}
