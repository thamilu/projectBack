package com.eshop.app.service;

import com.eshop.app.dto.response.AdminDashboardResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminDashboardService {

    private final com.eshop.app.service.ActivityService activityService;
    private final java.util.concurrent.Executor dashboardExecutor;
    private final com.eshop.app.service.analytics.AdminAggregationService adminAggregationService;

    @Cacheable(value = "adminDashboard", key = "'overview'", sync = true)
    public AdminDashboardResponse getDashboard() {
        long start = System.currentTimeMillis();
        try {
                AdminDashboardResponse.OverviewStats overview = adminAggregationService.getOverviewStats();
                AdminDashboardResponse.UserStats userStats = adminAggregationService.getUserStats();

                AdminDashboardResponse response = AdminDashboardResponse.builder()
                    .overview(overview)
                    .userStats(userStats)
                    .recentActivities(activityService.getRecentActivities(10))
                    .systemHealth(AdminDashboardResponse.SystemHealth.builder().status(AdminDashboardResponse.HealthStatus.HEALTHY).build())
                    .role("ADMIN")
                    .timestamp(Instant.now())
                    .generationTimeMs(System.currentTimeMillis() - start)
                    .build();

            return response;

        } catch (Exception e) {
            log.error("Failed to build admin dashboard: {}", e.getMessage(), e);
            return AdminDashboardResponse.builder()
                    .overview(AdminDashboardResponse.OverviewStats.builder().build())
                    .userStats(AdminDashboardResponse.UserStats.builder().build())
                    .recentActivities(List.of())
                    .systemHealth(AdminDashboardResponse.SystemHealth.builder().status(AdminDashboardResponse.HealthStatus.DEGRADED).build())
                    .role("ADMIN")
                    .timestamp(Instant.now())
                    .error("Partial failure building dashboard")
                    .build();
        }
    }

    @Async("dashboardExecutor")
    public CompletableFuture<AdminDashboardResponse> getDashboardAsync() {
        return CompletableFuture.supplyAsync(this::getDashboard, dashboardExecutor);
    }
}
