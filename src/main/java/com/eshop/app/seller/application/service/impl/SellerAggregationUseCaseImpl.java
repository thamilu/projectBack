package com.eshop.app.seller.application.service.impl;

import com.eshop.app.catalog.application.port.in.ProductUseCase;
import com.eshop.app.order.application.port.in.OrderAnalyticsUseCase;
import com.eshop.app.seller.api.response.SellerDashboardResponse;
import com.eshop.app.seller.api.response.SellerAggregationMetricsDTO;
import com.eshop.app.seller.application.port.in.SellerAggregationUseCase;
import com.eshop.app.store.application.port.in.StoreUseCase;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SellerAggregationUseCaseImpl implements SellerAggregationUseCase {
    private static final String STORE_STATUS_ACTIVE = "Active";

    private final ProductUseCase productService;
    private final OrderAnalyticsUseCase orderAnalyticsUseCase;
    private final StoreUseCase storeService;
    private final Executor dashboardExecutor;

    @Override
    public SellerDashboardResponse.StoreOverview buildStoreOverview(Long sellerId) {
        return SellerDashboardResponse.StoreOverview.builder()
                .storeName(storeService.getStoreNameBySellerId(sellerId))
                .storeStatus(STORE_STATUS_ACTIVE)
                .totalProducts(productService.getProductCountBySellerId(sellerId))
                .activeProducts(productService.getActiveProductCountBySellerId(sellerId))
                .outOfStockProducts(productService.getOutOfStockCountBySellerId(sellerId))
                .storeRating(storeService.getStoreRatingBySellerId(sellerId))
                .build();
    }

    @Override
    public SellerDashboardResponse.SalesMetrics buildSalesMetrics(Long sellerId,
            SellerAggregationMetricsDTO metrics) {
        if (metrics == null) {
            metrics = orderAnalyticsUseCase.getSellerAggregationMetrics(sellerId);
        }
        return SellerDashboardResponse.SalesMetrics.builder()
                .todaySales(metrics.getTodaySales())
                .weeklySales(metrics.getWeeklySales())
                .monthlySales(metrics.getMonthlySales())
                .totalSales(metrics.getTotalSales())
                .build();
    }

    @Override
    public SellerDashboardResponse.OrderManagement buildOrderManagement(Long sellerId,
            SellerAggregationMetricsDTO metrics) {
        if (metrics == null) {
            metrics = orderAnalyticsUseCase.getSellerAggregationMetrics(sellerId);
        }
        return SellerDashboardResponse.OrderManagement.builder()
                .newOrders(metrics.getNewOrders())
                .processingOrders(metrics.getProcessingOrders())
                .shippedOrders(metrics.getShippedOrders())
                .completedOrders(metrics.getCompletedOrders())
                .build();
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
