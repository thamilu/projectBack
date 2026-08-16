package com.eshop.app.order.application.dto;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

/**
 * Typed DTO carrying aggregated order statistics for a seller. Replaces the untyped Map returned by
 * repositories.
 */
@Getter
@Builder
public class SellerOrderStatisticsDTO {
    private final Long totalOrders;
    private final BigDecimal totalRevenue;
    private final Long pendingOrders;
    private final Long completedOrders;
    private final Long cancelledOrders;
    private final Long totalCustomers;
}
