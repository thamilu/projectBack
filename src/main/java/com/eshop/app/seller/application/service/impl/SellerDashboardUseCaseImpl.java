package com.eshop.app.seller.application.service.impl;

import com.eshop.app.catalog.application.port.in.ProductUseCase;
import com.eshop.app.order.application.port.in.OrderAnalyticsUseCase;
import com.eshop.app.seller.api.response.SellerDashboardResponse;
import com.eshop.app.seller.api.response.TopProductResponse;
import com.eshop.app.seller.api.response.RecentOrderResponse;
import com.eshop.app.seller.api.response.SellerAggregationMetricsDTO;
import com.eshop.app.seller.application.port.in.SellerDashboardUseCase;
import com.eshop.app.seller.application.port.in.SellerAggregationUseCase;
import com.eshop.app.seller.shared.exception.DashboardAggregationException;
import com.eshop.app.core.infrastructure.config.properties.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Service implementation orchestrating the aggregation of seller dashboard metrics snapshot.
 * Sequential execution is intentionally preferred over parallel streams to maintain ThreadLocal
 * JPA Transaction/Session boundary context safety across database calls.
 *
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SellerDashboardUseCaseImpl implements SellerDashboardUseCase {

    private final ProductUseCase productService;
    private final OrderAnalyticsUseCase orderAnalyticsUseCase;
    private final Executor dashboardExecutor;
    private final SellerAggregationUseCase sellerAggregationService;
    private final AppProperties appProperties;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public SellerDashboardResponse getDashboard(Long sellerId) {
        Objects.requireNonNull(sellerId, "Seller ID must not be null");
        if (sellerId <= 0) {
            throw new IllegalArgumentException("Seller ID must be positive");
        }

        log.info("Aggregating dashboard data snapshot for seller ID: {}", sellerId);

        try {
            SellerAggregationMetricsDTO metrics = orderAnalyticsUseCase
                    .getSellerAggregationMetrics(sellerId);

            SellerDashboardResponse.StoreOverview storeOverview = sellerAggregationService.buildStoreOverview(sellerId);
            SellerDashboardResponse.SalesMetrics sales = sellerAggregationService.buildSalesMetrics(sellerId, metrics);
            SellerDashboardResponse.OrderManagement om = sellerAggregationService.buildOrderManagement(sellerId, metrics);

            return SellerDashboardResponse.builder()
                    .storeOverview(storeOverview)
                    .salesMetrics(sales)
                    .orderManagement(om)
                    .topProducts(productService.getTopSellingProductsBySellerId(sellerId, 5).stream()
                            .map(p -> TopProductResponse.builder()
                                    .productId(p.getProductId())
                                    .productName(p.getProductName())
                                    .sku(p.getSku())
                                    .thumbnailUrl(p.getImageUrl())
                                    .categoryName(p.getCategoryName())
                                    .unitsSold(p.getTotalQuantitySold())
                                    .totalRevenue(p.getTotalRevenue())
                                    .build())
                            .toList())
                    .recentOrders(orderAnalyticsUseCase.getRecentOrdersBySellerId(sellerId, 10).stream()
                            .map(this::mapToRecentOrderResponse)
                            .filter(Objects::nonNull)
                            .toList())
                    .role(appProperties.getSecurity().getRoles().getSeller())
                    .timestamp(Instant.now(clock))
                    .build();
        } catch (Exception e) {
            log.error("Failed to aggregate seller dashboard data for sellerId={}", sellerId, e);
            throw new DashboardAggregationException(
                    "Failed to aggregate seller dashboard data asynchronously", e);
        }
    }

    @Override
    public CompletableFuture<SellerDashboardResponse> getDashboardAsync(Long sellerId) {
        return CompletableFuture.supplyAsync(() -> getDashboard(sellerId), dashboardExecutor);
    }

    /**
     * Defensive helper to map unstructured database result maps safely into typed response DTOs.
     */
    private RecentOrderResponse mapToRecentOrderResponse(Map<String, Object> map) {
        if (map == null) {
            return null;
        }
        return RecentOrderResponse.builder()
                .orderNumber(Objects.toString(map.get("orderNumber"), null))
                .status(Objects.toString(map.get("status"), null))
                .totalAmount(mapToBigDecimal(map.get("totalAmount")))
                .createdAt(mapToLocalDateTime(map.get("createdAt")))
                .build();
    }

    private BigDecimal mapToBigDecimal(Object val) {
        if (val instanceof BigDecimal) {
            return (BigDecimal) val;
        } else if (val instanceof Number) {
            return BigDecimal.valueOf(((Number) val).doubleValue());
        } else if (val != null) {
            return new BigDecimal(val.toString());
        }
        return null;
    }

    private LocalDateTime mapToLocalDateTime(Object val) {
        if (val instanceof LocalDateTime) {
            return (LocalDateTime) val;
        } else if (val instanceof java.sql.Timestamp) {
            return ((java.sql.Timestamp) val).toLocalDateTime();
        } else if (val instanceof java.util.Date) {
            return new java.sql.Timestamp(((java.util.Date) val).getTime()).toLocalDateTime();
        }
        return null;
    }
}
