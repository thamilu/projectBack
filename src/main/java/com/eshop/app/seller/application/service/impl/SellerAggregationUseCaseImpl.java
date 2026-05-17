package com.eshop.app.seller.application.service.impl;

import com.eshop.app.catalog.application.port.in.ProductUseCase;
import com.eshop.app.order.application.port.in.OrderAnalyticsUseCase;
import com.eshop.app.seller.api.response.SellerDashboardResponse;
import com.eshop.app.seller.api.response.SellerAggregationMetricsDTO;
import com.eshop.app.seller.application.port.in.SellerAggregationUseCase;
import com.eshop.app.store.application.port.in.StoreUseCase;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Service
@RequiredArgsConstructor
public class SellerAggregationUseCaseImpl implements SellerAggregationUseCase {
    private static final Logger log = LoggerFactory.getLogger(SellerAggregationUseCaseImpl.class);

    private final ProductUseCase productService;
    private final OrderAnalyticsUseCase orderAnalyticsUseCase;
    private final StoreUseCase storeService;
    private final Executor dashboardExecutor;

    @Override
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

    @Override
    public SellerDashboardResponse.SalesMetrics buildSalesMetrics(Long sellerId,
            SellerAggregationMetricsDTO metrics) {
        try {
            if (metrics == null)
                metrics = orderAnalyticsUseCase.getSellerAggregationMetrics(sellerId);
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

    @Override
    public SellerDashboardResponse.OrderManagement buildOrderManagement(Long sellerId,
            SellerAggregationMetricsDTO metrics) {
        try {
            if (metrics == null)
                metrics = orderAnalyticsUseCase.getSellerAggregationMetrics(sellerId);
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

    @Override
    public CompletableFuture<SellerDashboardResponse.StoreOverview> buildStoreOverviewAsync(Long sellerId) {
        return CompletableFuture.supplyAsync(() -> buildStoreOverview(sellerId), dashboardExecutor);
    }

    @Override
    public CompletableFuture<SellerDashboardResponse.SalesMetrics> buildSalesMetricsAsync(Long sellerId,
            SellerAggregationMetricsDTO metrics) {
        return CompletableFuture.supplyAsync(() -> buildSalesMetrics(sellerId, metrics), dashboardExecutor);
    }

    @Override
    public CompletableFuture<SellerDashboardResponse.OrderManagement> buildOrderManagementAsync(Long sellerId,
            SellerAggregationMetricsDTO metrics) {
        return CompletableFuture.supplyAsync(() -> buildOrderManagement(sellerId, metrics), dashboardExecutor);
    }
}
