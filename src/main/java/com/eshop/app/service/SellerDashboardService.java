package com.eshop.app.service;

import com.eshop.app.dto.response.SellerDashboardResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Service
@RequiredArgsConstructor
public class SellerDashboardService {

    private final com.eshop.app.service.ProductService productService;
    private final com.eshop.app.service.OrderService orderService;
    @SuppressWarnings("unused") // Reserved for future store-related features
    private final com.eshop.app.service.StoreService storeService;
        private final Executor dashboardExecutor;
        private final com.eshop.app.service.analytics.SellerAggregationService sellerAggregationService;

    public SellerDashboardResponse getDashboard(Long sellerId) {
        SellerDashboardResponse.StoreOverview storeOverview = sellerAggregationService.buildStoreOverview(sellerId);
        SellerDashboardResponse.SalesMetrics sales = sellerAggregationService.buildSalesMetrics(sellerId);
        SellerDashboardResponse.OrderManagement om = sellerAggregationService.buildOrderManagement(sellerId);

        return SellerDashboardResponse.builder()
                .storeOverview(storeOverview)
                .salesMetrics(sales)
                .orderManagement(om)
                .topProducts(productService.getTopSellingProductsBySellerId(sellerId, 5))
                .recentOrders(orderService.getRecentOrdersBySellerId(sellerId, 10))
                .role("SELLER")
                .timestamp(Instant.now())
                .build();
    }

        /**
         * Async wrapper for seller dashboard to allow controller orchestration.
         */
        public CompletableFuture<SellerDashboardResponse> getDashboardAsync(Long sellerId) {
                return CompletableFuture.supplyAsync(() -> getDashboard(sellerId), dashboardExecutor);
        }
}
