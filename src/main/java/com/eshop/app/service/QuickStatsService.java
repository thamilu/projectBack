package com.eshop.app.service;

import com.eshop.app.dto.response.QuickStatsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class QuickStatsService {

    private final OrderService orderService;
    private final ProductService productService;

    public QuickStatsResponse getStats(String role, Long userId) {
        QuickStatsResponse.QuickStatsResponseBuilder b = QuickStatsResponse.builder();
        switch (role) {
            case "ADMIN":
                b.pendingApprovals(5L)
                 .systemAlerts(2L)
                 .newRegistrations(12L);
                break;
            case "SELLER":
                b.newOrders(orderService.getNewOrderCountBySellerId(userId))
                 .lowStock(productService.getLowStockCountBySellerId(userId))
                 .todayRevenue(orderService.getTodayRevenueBySellerId(userId));
                break;
            case "CUSTOMER":
                b.cartItems(0L).wishlistItems(0L).orderStatus("No active orders");
                break;
            case "DELIVERY_AGENT":
                b.pendingDeliveries(orderService.getPendingDeliveriesByAgentId(userId))
                 .todayDeliveries(orderService.getTodayDeliveriesByAgentId(userId));
                break;
            default:
                break;
        }

        b.role(role);
        return b.build();
    }
}
