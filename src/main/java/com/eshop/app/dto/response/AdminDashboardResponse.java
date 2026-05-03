package com.eshop.app.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdminDashboardResponse {
    private OverviewStats overview;
    private UserStats userStats;
    private List<ActivityItem> recentActivities;
    private SystemHealth systemHealth;
    private String role;
    private Instant timestamp;
    private Long generationTimeMs;
    private String error;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OverviewStats {
        private Long totalUsers;
        private Long totalProducts;
        private Long totalStores;
        private Long totalOrders;
        private Long pendingOrders;
        private Long todayOrders;
        private BigDecimal totalRevenue;
        private BigDecimal monthlyRevenue;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserStats {
        private Long customers;
        private Long sellers;
        private Long deliveryAgents;
        private Long activeUsers;
        private Long newUsersThisMonth;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActivityItem {
        private String action;
        private String description;
        private String type;
        private Instant timestamp;
        private String relativeTime;
        private Long userId;
        private String userName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SystemHealth {
        private HealthStatus status;
        private String uptime;
        private Double uptimePercentage;
        private LoadLevel serverLoad;
        private ConnectionStatus databaseStatus;
        private ConnectionStatus cacheStatus;
        private ConnectionStatus messageQueueStatus;
    }

    public enum HealthStatus { HEALTHY, DEGRADED, UNHEALTHY }
    public enum LoadLevel { LOW, NORMAL, HIGH, CRITICAL }
    public enum ConnectionStatus { CONNECTED, DISCONNECTED, DEGRADED }
}
