package com.eshop.app.seller.application.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

import com.eshop.app.catalog.api.response.TopSellingProductResponse;
import com.eshop.app.catalog.application.port.in.ProductUseCase;
import com.eshop.app.order.application.port.in.OrderAnalyticsUseCase;
import com.eshop.app.seller.api.response.SellerAggregationMetricsDTO;
import com.eshop.app.seller.api.response.SellerDashboardResponse;
import com.eshop.app.seller.application.port.in.SellerAggregationUseCase;
import com.eshop.app.seller.shared.exception.DashboardAggregationException;
import com.eshop.app.core.infrastructure.config.properties.AppProperties;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SellerDashboardUseCaseImplTest {

    @Mock private ProductUseCase productService;

    @Mock private OrderAnalyticsUseCase orderAnalyticsUseCase;

    @Mock private SellerAggregationUseCase sellerAggregationService;

    @Mock private AppProperties appProperties;

    private Executor testExecutor = Runnable::run; // Synchronous execution for testing

    private Clock fixedClock;
    private Instant fixedInstant;

    private SellerDashboardUseCaseImpl sellerDashboardUseCase;

    @BeforeEach
    void setUp() {
        fixedInstant = Instant.parse("2026-07-02T12:00:00Z");
        fixedClock = Clock.fixed(fixedInstant, ZoneId.of("UTC"));

        AppProperties.Security.Roles roles = mock(AppProperties.Security.Roles.class);
        AppProperties.Security security = mock(AppProperties.Security.class);
        lenient().when(security.getRoles()).thenReturn(roles);
        lenient().when(roles.getSeller()).thenReturn("SELLER");
        lenient().when(appProperties.getSecurity()).thenReturn(security);

        sellerDashboardUseCase =
                new SellerDashboardUseCaseImpl(
                        productService,
                        orderAnalyticsUseCase,
                        testExecutor,
                        sellerAggregationService,
                        appProperties,
                        fixedClock);
    }

    @Test
    void getDashboard_SuccessfulAggregation_ReturnsDashboardResponse() {
        Long sellerId = 1L;

        SellerAggregationMetricsDTO mockMetrics = SellerAggregationMetricsDTO.builder().build();
        SellerDashboardResponse.StoreOverview mockStoreOverview =
                SellerDashboardResponse.StoreOverview.builder().build();
        List<TopSellingProductResponse> mockTopProducts = Collections.emptyList();
        List<Map<String, Object>> mockRecentOrders = Collections.emptyList();

        SellerDashboardResponse.SalesMetrics mockSalesMetrics =
                SellerDashboardResponse.SalesMetrics.builder().build();
        SellerDashboardResponse.OrderManagement mockOrderManagement =
                SellerDashboardResponse.OrderManagement.builder().build();

        when(orderAnalyticsUseCase.getSellerAggregationMetrics(sellerId)).thenReturn(mockMetrics);
        when(sellerAggregationService.buildStoreOverview(sellerId)).thenReturn(mockStoreOverview);
        when(productService.getTopSellingProductsBySellerId(sellerId, 5))
                .thenReturn(mockTopProducts);
        when(orderAnalyticsUseCase.getRecentOrdersBySellerId(sellerId, 10))
                .thenReturn(mockRecentOrders);

        when(sellerAggregationService.buildSalesMetrics(sellerId, mockMetrics))
                .thenReturn(mockSalesMetrics);
        when(sellerAggregationService.buildOrderManagement(sellerId, mockMetrics))
                .thenReturn(mockOrderManagement);

        SellerDashboardResponse response = sellerDashboardUseCase.getDashboard(sellerId);

        assertThat(response).isNotNull();
        assertThat(response.getStoreOverview()).isEqualTo(mockStoreOverview);
        assertThat(response.getSalesMetrics()).isEqualTo(mockSalesMetrics);
        assertThat(response.getOrderManagement()).isEqualTo(mockOrderManagement);
        assertThat(response.getTopProducts()).isEmpty();
        assertThat(response.getRecentOrders()).isEmpty();
        assertThat(response.getRole()).isEqualTo("SELLER");
        assertThat(response.getTimestamp()).isEqualTo(fixedInstant);
    }

    @Test
    void getDashboard_AsyncFailure_ThrowsDashboardAggregationException() {
        Long sellerId = 1L;

        when(orderAnalyticsUseCase.getSellerAggregationMetrics(sellerId))
                .thenThrow(new RuntimeException("Database error on metrics fetch"));

        assertThatThrownBy(() -> sellerDashboardUseCase.getDashboard(sellerId))
                .isInstanceOf(DashboardAggregationException.class)
                .hasMessageContaining("Failed to aggregate seller dashboard data asynchronously")
                .hasCauseInstanceOf(RuntimeException.class);
    }
}
