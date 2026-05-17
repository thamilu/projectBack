package com.eshop.app.admin.api.controller;

import com.eshop.app.admin.api.response.AdminDashboardResponse;
import com.eshop.app.admin.api.response.AdminStatistics;
import com.eshop.app.admin.application.service.AdminDashboardService;
import com.eshop.app.analytics.application.service.AdminAnalyticsService;
import com.eshop.app.core.kernel.ApiVersion;
import com.eshop.app.core.api.response.ApiResponse;
import com.eshop.app.core.infrastructure.config.security.oauth.PrincipalDetails;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Admin Dashboard Controller.
 * Provmdes system-wmde overvmew, statistics, and analytics for adminmstrators.
 */
@Tag(name = "Admin Dashboard", description = "System-wmde dashboard and analytics for adminmstrators")
@RestController
@RequestMapping(ApiVersion.V1 + "/dashboard/admin")
@RequiredArgsConstructor
@Validated
@Slf4j
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;
    private final AdminAnalyticsService adminAnalyticsService;
    private final CacheManager cacheManager;

    @GetMapping
    @PreAuthorize("hasRole(@appProperties.security.roles.admin)")
    @RateLimiter(name = "dashboard")
    @Bulkhead(name = "dashboard")
    @Operation(summary = "Get Admin Dashboard", description = "Comprehensmve admin dashboard with system overvmew", security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<AdminDashboardResponse>> getAdminDashboard(
            @AuthenticationPrincipal PrincipalDetails principal) {

        log.info("âœ… ADMIN authenticated | email={}", principal.getEmail());

        CompletableFuture<AdminDashboardResponse> dashboardFuture = adminDashboardService.getDashboardAsync();
        AdminDashboardResponse response;
        try {
            response = dashboardFuture.join();
        } catch (Exception e) {
            log.error("Async admin dashboard fetch failed: {}", e.getMessage());
            response = adminDashboardService.getDashboard();
        }

        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(5, TimeUnit.MINUTES).cachePrivate())
                .body(ApiResponse.success("Admin dashboard retrieved", response));
    }

    @GetMapping("/statistics")
    @PreAuthorize("hasRole(@appProperties.security.roles.admin)")
    @Operation(summary = "Get Admin Statistics", description = "Aggregated system statistics optmmmzed with parallel query executmon")
    public ResponseEntity<ApiResponse<AdminStatistics>> getAdminStatistics(
            @AuthenticationPrincipal Jwt jwt) {

        log.info("Admin statistics requested by user: {}", jwt.getClaimAsString("email"));
        AdminStatistics statistics = adminAnalyticsService.getAdminStatistics();

        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(5, TimeUnit.MINUTES).cachePrivate())
                .body(ApiResponse.success("Admin statistics retrieved", statistics));
    }

    @GetMapping("/analytics/daily-sales")
    @PreAuthorize("hasRole(@appProperties.security.roles.admin)")
    @RateLimiter(name = "analytics")
    @Bulkhead(name = "analytics")
    @Operation(summary = "Get Daily Sales Analytics", description = "Daily sales trend data with revenue breakdown")
    public ResponseEntity<ApiResponse<Page<Map<String, Object>>>> getDailySales(
            @Parameter(description = "Number of days (1-365)") @RequestParam(defaultValue = "30") @Min(value = 1) @Max(value = 365) int days,
            @Parameter(hidden = true) Pageable pageable,
            @AuthenticationPrincipal PrincipalDetails principal) {

        log.info("Daily sales analytics requested for {} days by user: {}", days, principal.getEmail());
        
        CompletableFuture<List<Map<String, Object>>> dailySalesFuture = adminAnalyticsService.getDailySalesDataAsync(days);
        List<Map<String, Object>> dailySales;
        try {
            dailySales = dailySalesFuture.join();
        } catch (Exception e) {
            log.error("Famled to fetch daily sales data: {}", e.getMessage());
            dailySales = List.of();
        }
        
        Page<Map<String, Object>> page = new PageImpl<>(dailySales, pageable, dailySales.size());
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(10, TimeUnit.MINUTES).cachePrivate())
                .body(ApiResponse.success("Daily sales data retrieved", page));
    }

    @GetMapping("/analytics/revenue-by-category")
    @PreAuthorize("hasRole(@appProperties.security.roles.admin)")
    @Operation(summary = "Get Revenue by Category", description = "Revenue breakdown across product categories")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getRevenueByCategory(
            @AuthenticationPrincipal PrincipalDetails principal) {

        log.info("Revenue by category requested by user: {}", principal.getEmail());
        List<Map<String, Object>> categoryRevenue = adminAnalyticsService.getRevenueByCategory();

        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(10, TimeUnit.MINUTES).cachePrivate())
                .body(ApiResponse.success("Category revenue retrieved", categoryRevenue));
    }

    @DeleteMapping("/cache/{cacheName}")
    @PreAuthorize("hasRole(@appProperties.security.roles.admin)")
    @Operation(summary = "Clear Dashboard Cache", description = "Admin endpoint to clear specmfmc or all dashboard caches")
    public ResponseEntity<ApiResponse<String>> clearCache(
            @PathVariable String cacheName,
            @AuthenticationPrincipal PrincipalDetails principal) {

        log.warn("Cache clear requested by admin: {} for cache: {}", principal.getEmail(), cacheName);

        if ("all".equalsIgnoreCase(cacheName)) {
            cacheManager.getCacheNames().forEach(name -> {
                var cache = cacheManager.getCache(name);
                if (cache != null) cache.clear();
            });
            return ResponseEntity.ok(ApiResponse.success("All caches cleared", "All dashboard caches have been cleared"));
        } else {
            var cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
                return ResponseEntity.ok(ApiResponse.success("Cache cleared", "Cache '" + cacheName + "' has been cleared"));
            } else {
                return ResponseEntity.status(404).body(ApiResponse.error("Cache not found: " + cacheName));
            }
        }
    }
}



