package com.eshop.app.service.analytics;

import com.eshop.app.dto.response.AdminDashboardResponse;
import com.eshop.app.service.OrderService;
import com.eshop.app.service.ProductService;
import com.eshop.app.service.StoreService;
import com.eshop.app.service.UserService;
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
    ProductService productService;

    @Mock
    StoreService storeService;

    @Mock
    OrderService orderService;

    Executor dashboardExecutor = r -> r.run();

    @InjectMocks
    AdminAggregationService aggregationService;

    @Test
    void getOverviewStats_returnsNonNull() {
        when(userService.getTotalUserCount()).thenReturn(10L);
        when(productService.getTotalProductCount()).thenReturn(20L);
        when(storeService.getTotalStoreCount()).thenReturn(3L);
        when(orderService.getTotalOrderCount()).thenReturn(5L);
        when(orderService.getPendingOrderCount()).thenReturn(1L);
        when(orderService.getTodayOrderCount()).thenReturn(0L);

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
