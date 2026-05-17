package com.eshop.app.order.application.port.in;

import com.eshop.app.seller.api.response.SellerAggregationMetricsDTO;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Use case for order analytics and statistics.
 */
public interface OrderAnalyticsUseCase {
    long getTotalOrderCount();
    long getPendingOrderCount();
    long getTodayOrderCount();
    BigDecimal getTotalRevenue();
    BigDecimal getMonthlyRevenue();

    // Seller-specific
    BigDecimal getTodayRevenueBySellerId(Long sellerId);
    BigDecimal getWeeklyRevenueBySellerId(Long sellerId);
    BigDecimal getMonthlyRevenueBySellerId(Long sellerId);
    BigDecimal getTotalRevenueBySellerId(Long sellerId);
    long getNewOrderCountBySellerId(Long sellerId);
    long getProcessingOrderCountBySellerId(Long sellerId);
    long getShippedOrderCountBySellerId(Long sellerId);
    long getCompletedOrderCountBySellerId(Long sellerId);
    SellerAggregationMetricsDTO getSellerAggregationMetrics(Long sellerId);
    List<Map<String, Object>> getRecentOrdersBySellerId(Long sellerId, int limit);
    Map<String, BigDecimal> getRevenueBreakdownBySellerId(Long sellerId);

    // Customer-specific
    long getOrderCountByCustomerId(Long customerId);
    List<Map<String, Object>> getRecentOrdersByCustomerId(Long customerId, int limit);
    BigDecimal getTotalSpentByCustomerId(Long customerId);
    BigDecimal getAverageOrderValueByCustomerId(Long customerId);

    // Delivery-specific
    long getPendingDeliveriesByAgentId(Long agentId);
    long getTodayDeliveriesByAgentId(Long agentId);
    long getInTransitOrdersByAgentId(Long agentId);
    long getCompletedDeliveriesTodayByAgentId(Long agentId);
    long getCompletedDeliveriesThisWeekByAgentId(Long agentId);
    long getCompletedDeliveriesThisMonthByAgentId(Long agentId);
    double getDeliverySuccessRateByAgentId(Long agentId);
    List<Map<String, Object>> getRecentDeliveriesByAgentId(Long agentId, int limit);
    long getUrgentDeliveriesByAgentId(Long agentId);
    double getAverageDeliveryTimeByAgentId(Long agentId);
    double getCustomerRatingByAgentId(Long agentId);
}
