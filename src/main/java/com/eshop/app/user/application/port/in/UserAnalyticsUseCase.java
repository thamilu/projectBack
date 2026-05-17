package com.eshop.app.user.application.port.in;

import java.util.List;
import java.util.Map;

/**
 * Use case for user-related analytics.
 */
public interface UserAnalyticsUseCase {
    long getTotalUserCount();
    long getCustomerCount();
    long getSellerCount();
    long getDeliveryAgentCount();
    long getActiveUserCount();
    long getNewUsersThisMonth();
    List<Map<String, Object>> getUserGrowthData();
}
