package com.eshop.app.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for aggregated admin statistics.
 * Optimized for single-query aggregation.
 * 
 * @author EShop Team
 * @since 2.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminStatistics {
    
    private Long totalUsers;
    private Long totalCustomers;
    private Long totalSellers;
    private Long totalDeliveryAgents;
    private Long activeUsers;
    private Long newUsersThisMonth;
    
    private Long totalProducts;
    private Long activeProducts;
    private Long outOfStockProducts;
    
    private Long totalOrders;
    private Long pendingOrders;
    private Long completedOrders;
    private Long todayOrders;
    
    private BigDecimal totalRevenue;
    private BigDecimal monthlyRevenue;
    private BigDecimal todayRevenue;
    
    private Long totalShops;
    private Long activeShops;
}
