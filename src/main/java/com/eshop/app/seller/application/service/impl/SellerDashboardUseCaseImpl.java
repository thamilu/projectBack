package com.eshop.app.seller.application.service.impl;

import com.eshop.app.seller.api.response.SellerDashboardResponse;
import com.eshop.app.seller.application.port.in.SellerDashboardUseCase;
import com.eshop.app.seller.application.port.in.SellerAggregationUseCase;
import com.eshop.app.store.application.port.in.StoreUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Service
@RequiredArgsConstructor
public class SellerDashboardUseCaseImpl implements SellerDashboardUseCase {

    private final com.eshop.app.catalog.application.port.in.ProductUseCase productService;
    private final com.eshop.app.order.application.port.in.OrderAnalyticsUseCase orderAnalyticsUseCase;
    @SuppressWarnings("unused")
    private final StoreUseCase storeService;
    private final Executor dashboardExecutor;
    private final SellerAggregationUseCase sellerAggregationService;

    @Override
    public SellerDashboardResponse getDashboard(Long sellerId) {
        com.eshop.app.seller.api.response.SellerAggregationMetricsDTO metrics = orderAnalyticsUseCase
                .getSellerAggregationMetrics(sellerId);

        SellerDashboardResponse.StoreOverview storeOverview = sellerAggregationService.buildStoreOverview(sellerId);
        SellerDashboardResponse.SalesMetrics sales = sellerAggregationService.buildSalesMetrics(sellerId, metrics);
        SellerDashboardResponse.OrderManagement om = sellerAggregationService.buildOrderManagement(sellerId, metrics);

        return SellerDashboardResponse.builder()
                .storeOverview(storeOverview)
                .salesMetrics(sales)
                .orderManagement(om)
                .topProducts(productService.getTopSellingProductsBySellerId(sellerId, 5))
                .recentOrders(orderAnalyticsUseCase.getRecentOrdersBySellerId(sellerId, 10))
                .role("SELLER")
                .timestamp(Instant.now())
                .build();
    }

    @Override
    public CompletableFuture<SellerDashboardResponse> getDashboardAsync(Long sellerId) {
        return CompletableFuture.supplyAsync(() -> getDashboard(sellerId), dashboardExecutor);
    }
}
