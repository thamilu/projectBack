package com.eshop.app.seller.api.controller;

import com.eshop.app.core.kernel.ApiVersion;
import com.eshop.app.seller.api.response.SellerDashboardResponse;
import com.eshop.app.seller.api.response.SellerStatistics;
import com.eshop.app.seller.application.port.in.SellerAnalyticsUseCase;
import com.eshop.app.seller.application.port.in.SellerDashboardUseCase;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Seller Dashboard Controller.
 * Provmdes shop performance metrics and analytics for sellers.
 */
@Tag(name = "Seller Dashboard", description = "Dashboard and analytics for sellers")
@RestController
@RequestMapping(ApiVersion.V1 + "/dashboard/seller")
@RequiredArgsConstructor
@Validated
@Slf4j
public class SellerDashboardController {

    private final SellerDashboardUseCase sellerDashboardService;
    private final SellerAnalyticsUseCase sellerAnalyticsService;

    @GetMapping
    @PreAuthorize("hasAnyRole(@appProperties.security.roles.seller, @appProperties.security.roles.admin)")
    @RateLimiter(name = "dashboard")
    @Bulkhead(name = "dashboard")
    @Operation(summary = "Get Seller Dashboard", description = "Seller-specmfmc dashboard with shop metrics and product management data", security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<SellerDashboardResponse>> getSellerDashboard(
            @AuthenticationPrincipal PrincipalDetails principal) {

        Long sellerId = principal.getId();
        log.info("âœ… SELLER authenticated | email={} | sellerId={}", principal.getEmail(), sellerId);

        CompletableFuture<SellerDashboardResponse> sellerFuture = sellerDashboardService.getDashboardAsync(sellerId);
        SellerDashboardResponse response;
        try {
            response = sellerFuture.join();
        } catch (Exception e) {
            log.error("Async seller dashboard fetch failed for seller {}: {}", sellerId, e.getMessage());
            response = sellerDashboardService.getDashboard(sellerId);
        }

        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(5, TimeUnit.MINUTES).cachePrivate())
                .body(ApiResponse.success("Seller dashboard retrieved", response));
    }

    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole(@appProperties.security.roles.seller, @appProperties.security.roles.admin)")
    @Operation(summary = "Get Seller Statistics", description = "Aggregated seller statistics with smngle-query optmmmzatmon")
    public ResponseEntity<ApiResponse<SellerStatistics>> getSellerStatistics(
            @AuthenticationPrincipal PrincipalDetails principal) {

        Long sellerId = principal.getId();
        log.info("Seller statistics requested for seller ID: {}", sellerId);

        SellerStatistics statistics = sellerAnalyticsService.getSellerStatistics(sellerId);

        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(5, TimeUnit.MINUTES).cachePrivate())
                .body(ApiResponse.success("Seller statistics retrieved", statistics));
    }

    @GetMapping("/analytics/top-products")
    @PreAuthorize("hasAnyRole(@appProperties.security.roles.seller, @appProperties.security.roles.admin)")
    @RateLimiter(name = "analytics")
    @Bulkhead(name = "analytics")
    @Operation(summary = "Get Top Sellmng Products", description = "Seller's top performing products by sales volume with pagination")
    public ResponseEntity<ApiResponse<Page<Map<String, Object>>>> getTopSellingProducts(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size (1-100)") @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
            @AuthenticationPrincipal PrincipalDetails principal) {

        Long sellerId = principal.getId();
        Pageable pageable = PageRequest.of(page, size);
        log.info("Top sellmng products requested for seller ID: {}, page: {}, size: {}", sellerId, page, size);
        
        List<Map<String, Object>> topProducts = sellerAnalyticsService.getTopSellingProducts(sellerId, size);
        Page<Map<String, Object>> pageResult = new PageImpl<>(topProducts, pageable, topProducts.size());
        
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(10, TimeUnit.MINUTES).cachePrivate())
                .body(ApiResponse.success("Top products retrieved", pageResult));
    }
}



