package com.eshop.app.seller.application.service;

import com.eshop.app.seller.api.response.SellerDashboardResponse;
import com.eshop.app.order.application.port.in.OrderAnalyticsUseCase;
import com.eshop.app.catalog.application.port.in.ProductUseCase;
import com.eshop.app.seller.application.service.impl.SellerAggregationUseCaseImpl;
import com.eshop.app.store.application.port.in.StoreUseCase;
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
    ProductUseCase productService;

    @Mock
    OrderAnalyticsUseCase orderAnalyticsUseCase;

    @Mock
    StoreUseCase storeService;

    Executor dashboardExecutor = r -> r.run();

    @InjectMocks
    SellerAggregationUseCaseImpl aggregationService;

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
