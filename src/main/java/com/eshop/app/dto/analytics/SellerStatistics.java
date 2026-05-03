package com.eshop.app.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for aggregated seller statistics.
 * Optimized to be populated in a single database query.
 * 
 * @author EShop Team
 * @since 2.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellerStatistics {
    
    private Long totalOrders;
    private BigDecimal totalRevenue;
    private Long pendingOrders;
    private Long completedOrders;
    private Long cancelledOrders;
    private Long totalProducts;
    private Long activeProducts;
    private Double averageRating;
    private Long totalCustomers;
    private BigDecimal monthlyRevenue;
    private Long monthlyOrders;
}
