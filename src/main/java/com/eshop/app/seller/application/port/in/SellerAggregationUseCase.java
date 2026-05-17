package com.eshop.app.seller.application.port.in;

import com.eshop.app.seller.api.response.SellerDashboardResponse;
import com.eshop.app.seller.api.response.SellerAggregationMetricsDTO;
import java.util.concurrent.CompletableFuture;

/**
 * Inbound Port for Seller Aggregation Use Cases.
 */
public interface SellerAggregationUseCase {
    SellerDashboardResponse.StoreOverview buildStoreOverview(Long sellerId);
    SellerDashboardResponse.SalesMetrics buildSalesMetrics(Long sellerId, SellerAggregationMetricsDTO metrics);
    SellerDashboardResponse.OrderManagement buildOrderManagement(Long sellerId, SellerAggregationMetricsDTO metrics);
    CompletableFuture<SellerDashboardResponse.StoreOverview> buildStoreOverviewAsync(Long sellerId);
    CompletableFuture<SellerDashboardResponse.SalesMetrics> buildSalesMetricsAsync(Long sellerId, SellerAggregationMetricsDTO metrics);
    CompletableFuture<SellerDashboardResponse.OrderManagement> buildOrderManagementAsync(Long sellerId, SellerAggregationMetricsDTO metrics);
}
