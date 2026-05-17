package com.eshop.app.shipping.application.service.impl;

import com.eshop.app.shipping.api.response.DeliveryDashboardResponse;
import com.eshop.app.shipping.application.port.in.DeliveryDashboardUseCase;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class DeliveryDashboardUseCaseImpl implements DeliveryDashboardUseCase {

        private final com.eshop.app.order.application.port.in.OrderAnalyticsUseCase orderAnalyticsUseCase;

        @Override
        public DeliveryDashboardResponse getDashboard(Long agentId) {
                DeliveryDashboardResponse.Assignments assignments = DeliveryDashboardResponse.Assignments.builder()
                                .pendingDeliveries(orderAnalyticsUseCase.getPendingDeliveriesByAgentId(agentId))
                                .todayDeliveries(orderAnalyticsUseCase.getTodayDeliveriesByAgentId(agentId))
                                .inTransitOrders(orderAnalyticsUseCase.getInTransitOrdersByAgentId(agentId))
                                .urgentDeliveries(orderAnalyticsUseCase.getUrgentDeliveriesByAgentId(agentId))
                                .build();

                DeliveryDashboardResponse.Performance perf = DeliveryDashboardResponse.Performance.builder()
                                .completedToday(orderAnalyticsUseCase.getCompletedDeliveriesTodayByAgentId(agentId))
                                .completedThisWeek(orderAnalyticsUseCase.getCompletedDeliveriesThisWeekByAgentId(agentId))
                                .completedThisMonth(orderAnalyticsUseCase.getCompletedDeliveriesThisMonthByAgentId(agentId))
                                .averageDeliveryTime(
                                                String.valueOf(orderAnalyticsUseCase.getAverageDeliveryTimeByAgentId(agentId)))
                                .successRate(orderAnalyticsUseCase.getDeliverySuccessRateByAgentId(agentId))
                                .customerRating(orderAnalyticsUseCase.getCustomerRatingByAgentId(agentId))
                                .build();

                DeliveryDashboardResponse.RouteInfo route = DeliveryDashboardResponse.RouteInfo.builder()
                                .optimizedRoute("Available in next update")
                                .estimatedTime("Calculating...")
                                .totalDistance("Calculating...")
                                .build();

                return DeliveryDashboardResponse.builder()
                                .assignments(assignments)
                                .performance(perf)
                                .recentDeliveries(orderAnalyticsUseCase.getRecentDeliveriesByAgentId(agentId, 10))
                                .routeInfo(route)
                                .role("DELIVERY_AGENT")
                                .timestamp(Instant.now())
                                .build();
        }
}
