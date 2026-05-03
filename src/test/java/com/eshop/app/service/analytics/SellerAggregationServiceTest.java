package com.eshop.app.service.analytics;

import com.eshop.app.dto.response.SellerDashboardResponse;
import com.eshop.app.service.OrderService;
import com.eshop.app.service.ProductService;
import com.eshop.app.service.StoreService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SellerAggregationServiceTest {

    @Mock
    ProductService productService;

    @Mock
    OrderService orderService;

    @Mock
    StoreService storeService;

    Executor dashboardExecutor = r -> r.run();

    @InjectMocks
    SellerAggregationService aggregationService;

    @Test
    void buildStoreOverview_returnsNonNull() {
        when(storeService.getStoreNameBySellerId(1L)).thenReturn("Demo Store");
        when(productService.getProductCountBySellerId(1L)).thenReturn(5L);
        when(productService.getActiveProductCountBySellerId(1L)).thenReturn(4L);
        when(productService.getOutOfStockCountBySellerId(1L)).thenReturn(1L);
        when(storeService.getStoreRatingBySellerId(1L)).thenReturn(4.5);

        SellerDashboardResponse.StoreOverview ov = aggregationService.buildStoreOverview(1L);
        assertNotNull(ov);
    }

}
