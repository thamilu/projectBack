package com.eshop.app.admin.application.service;

import com.eshop.app.admin.api.response.AdminDashboardResponse;
import com.eshop.app.order.application.port.in.OrderAnalyticsUseCase;
import com.eshop.app.catalog.application.port.in.ProductUseCase;
import com.eshop.app.store.application.port.in.StoreUseCase;
import com.eshop.app.user.application.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class AdminAggregationServiceTest {

    @Mock
    UserService userService;

    @Mock
    ProductUseCase productService;

    @Mock
    StoreUseCase storeService;

    @Mock
    OrderAnalyticsUseCase orderAnalyticsUseCase;

    Executor dashboardExecutor = r -> r.run();

    @InjectMocks
    AdminAggregationService aggregationService;

    @Test
    void getOverviewStats_returnsNonNull() {
        when(userService.getTotalUserCount()).thenReturn(10L);
        when(productService.getTotalProductCount()).thenReturn(20L);
        when(storeService.getTotalStoreCount()).thenReturn(3L);
        when(orderAnalyticsUseCase.getTotalOrderCount()).thenReturn(5L);
        when(orderAnalyticsUseCase.getPendingOrderCount()).thenReturn(1L);
        when(orderAnalyticsUseCase.getTodayOrderCount()).thenReturn(0L);

        AdminDashboardResponse.OverviewStats overview = aggregationService.getOverviewStats();

        assertNotNull(overview);
    }

    @Test
    void getUserStats_returnsNonNull() {
        when(userService.getCustomerCount()).thenReturn(5L);
        when(userService.getSellerCount()).thenReturn(2L);
        when(userService.getDeliveryAgentCount()).thenReturn(1L);
        when(userService.getActiveUserCount()).thenReturn(4L);
        when(userService.getNewUsersThisMonth()).thenReturn(1L);

        AdminDashboardResponse.UserStats stats = aggregationService.getUserStats();

        assertNotNull(stats);
    }
}
