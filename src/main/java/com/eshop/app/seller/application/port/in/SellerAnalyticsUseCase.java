package com.eshop.app.seller.application.port.in;

import com.eshop.app.seller.api.response.SellerStatistics;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Inbound Port for Seller Analytics Use Cases.
 */
public interface SellerAnalyticsUseCase {
    SellerStatistics getSellerStatistics(Long sellerId);
    CompletableFuture<SellerStatistics> getSellerStatisticsAsync(Long sellerId);
    List<Map<String, Object>> getTopSellingProducts(Long sellerId, int limit);
    CompletableFuture<List<Map<String, Object>>> getTopSellingProductsAsync(Long sellerId, int limit);
    List<Map<String, Object>> getSalesTrend(Long sellerId, LocalDateTime startDate, LocalDateTime endDate);
}
