package com.eshop.app.user.application.service;

import com.eshop.app.catalog.application.port.in.ProductUseCase;
import com.eshop.app.order.application.port.in.OrderAnalyticsUseCase;
import com.eshop.app.core.api.response.QuickStatsResponse;




import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class QuickStatsService {

    private final OrderAnalyticsUseCase orderAnalyticsUseCase;
    private final ProductUseCase productService;

    public QuickStatsResponse getStats(String role, Long userId) {
        QuickStatsResponse.QuickStatsResponseBuilder b = QuickStatsResponse.builder();
        switch (role) {
            case "ADMIN":
                b.pendingApprovals(5L)
                 .systemAlerts(2L)
                 .newRegistrations(12L);
                break;
            case "SELLER":
                b.newOrders(orderAnalyticsUseCase.getNewOrderCountBySellerId(userId))
                 .lowStock(productService.getLowStockCountBySellerId(userId))
                 .todayRevenue(orderAnalyticsUseCase.getTodayRevenueBySellerId(userId));
                break;
            case "CUSTOMER":
                b.cartItems(0L).wishlistItems(0L).orderStatus("No active orders");
                break;
            case "DELIVERY_AGENT":
                b.pendingDeliveries(orderAnalyticsUseCase.getPendingDeliveriesByAgentId(userId))
                 .todayDeliveries(orderAnalyticsUseCase.getTodayDeliveriesByAgentId(userId));
                break;
            default:
                break;
        }

        b.role(role);
        return b.build();
    }
}


