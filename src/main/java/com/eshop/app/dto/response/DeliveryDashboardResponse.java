package com.eshop.app.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DeliveryDashboardResponse {
    private Assignments assignments;
    private Performance performance;
    private List<?> recentDeliveries;
    private RouteInfo routeInfo;
    private String role;
    private Instant timestamp;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Assignments {
        private Long pendingDeliveries;
        private Long todayDeliveries;
        private Long inTransitOrders;
        private Long urgentDeliveries;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Performance {
        private Long completedToday;
        private Long completedThisWeek;
        private Long completedThisMonth;
        private String averageDeliveryTime;
        private Double successRate;
        private Double customerRating;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RouteInfo {
        private String optimizedRoute;
        private String estimatedTime;
        private String totalDistance;
    }
}
