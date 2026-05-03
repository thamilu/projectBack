package com.eshop.app.dto.response;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuickStatsResponse {
    private String role;

    // Admin
    private Long pendingApprovals;
    private Long systemAlerts;
    private Long newRegistrations;

    // Seller
    private Long newOrders;
    private Long lowStock;
    private java.math.BigDecimal todayRevenue;

    // Customer
    private Long cartItems;
    private Long wishlistItems;
    private String orderStatus;

    // Delivery
    private Long pendingDeliveries;
    private Long todayDeliveries;
}
