package com.eshop.app.service;

import com.eshop.app.dto.response.DeliveryDashboardResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class DeliveryDashboardService {

    private final com.eshop.app.service.OrderService orderService;

    public DeliveryDashboardResponse getDashboard(Long agentId) {
        DeliveryDashboardResponse.Assignments assignments = DeliveryDashboardResponse.Assignments.builder()
                .pendingDeliveries(orderService.getPendingDeliveriesByAgentId(agentId))
                .todayDeliveries(orderService.getTodayDeliveriesByAgentId(agentId))
                .inTransitOrders(orderService.getInTransitOrdersByAgentId(agentId))
                .urgentDeliveries(orderService.getUrgentDeliveriesByAgentId(agentId))
                .build();

        DeliveryDashboardResponse.Performance perf = DeliveryDashboardResponse.Performance.builder()
                .completedToday(orderService.getCompletedDeliveriesTodayByAgentId(agentId))
                .completedThisWeek(orderService.getCompletedDeliveriesThisWeekByAgentId(agentId))
                .completedThisMonth(orderService.getCompletedDeliveriesThisMonthByAgentId(agentId))
                .averageDeliveryTime(String.valueOf(orderService.getAverageDeliveryTimeByAgentId(agentId)))
                .successRate(orderService.getDeliverySuccessRateByAgentId(agentId))
                .customerRating(orderService.getCustomerRatingByAgentId(agentId))
                .build();

        DeliveryDashboardResponse.RouteInfo route = DeliveryDashboardResponse.RouteInfo.builder()
                .optimizedRoute("Available in next update")
                .estimatedTime("Calculating...")
                .totalDistance("Calculating...")
                .build();

        return DeliveryDashboardResponse.builder()
                .assignments(assignments)
                .performance(perf)
                .recentDeliveries(orderService.getRecentDeliveriesByAgentId(agentId, 10))
                .routeInfo(route)
                .role("DELIVERY_AGENT")
                .timestamp(Instant.now())
                .build();
    }
}
