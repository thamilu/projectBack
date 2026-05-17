package com.eshop.app.payment.api.controller;

import com.eshop.app.core.kernel.ApiConstants;
import com.eshop.app.payment.application.service.PaymentAnalyticsService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Payment Analytics Controller
 * Provides comprehensive payment analytics and reporting endpoints
 */
@RestController
@RequestMapping(ApiConstants.Endpoints.PAYMENT_ANALYTICS)
@RequiredArgsConstructor
@io.swagger.v3.oas.annotations.tags.Tag(name = "Payment Analytics", description = "Payment analytics and reporting")
@SecurityRequirement(name = "Keycloak OAuth2")
@SecurityRequirement(name = "Bearer Authentication")
public class PaymentAnalyticsController {

    private final PaymentAnalyticsService paymentAnalyticsService;

    @GetMapping("/dashboard")
    @Operation(summary = "Get Payment Dashboard", description = "Get comprehensive payment dashboard with key metrics")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Payment dashboard data")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<Map<String, Object>> getPaymentDashboard() {
        Map<String, Object> dashboard = paymentAnalyticsService.getPaymentDashboard();
        return ResponseEntity.ok(dashboard);
    }

    @GetMapping("/gateways")
    @Operation(summary = "Get Gateway Statistics", description = "Get payment statistics by gateway")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Gateway statistics")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<Map<String, Object>> getGatewayStatistics() {
        Map<String, Object> stats = paymentAnalyticsService.getGatewayStatistics();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/payment-methods")
    @Operation(summary = "Get Payment Method Statistics", description = "Get payment distribution by payment methods")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Payment method statistics")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<Map<String, Object>> getPaymentMethodStatistics() {
        Map<String, Object> stats = paymentAnalyticsService.getPaymentMethodStatistics();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/trends/weekly")
    @Operation(summary = "Get Weekly Trends", description = "Get payment trends for the last 7 days")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Weekly payment trends")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<Object> getWeeklyTrends() {
        return ResponseEntity.ok(paymentAnalyticsService.getWeeklyTrend());
    }

    @GetMapping("/trends/hourly")
    @Operation(summary = "Get Hourly Trends", description = "Get hourly payment trends for today")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Hourly payment trends")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<Object> getHourlyTrends() {
        return ResponseEntity.ok(paymentAnalyticsService.getHourlyTrendToday());
    }

    @GetMapping("/regions")
    @Operation(summary = "Get Regional Performance", description = "Get payment performance by regions")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Regional payment performance")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<Map<String, Object>> getRegionalPerformance() {
        Map<String, Object> performance = paymentAnalyticsService.getPaymentPerformanceByRegion();
        return ResponseEntity.ok(performance);
    }

    @GetMapping("/failed-payments")
    @Operation(summary = "Get Failed Payments Analysis", description = "Get analysis of failed payments and failure reasons")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Failed payments analysis")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<Map<String, Object>> getFailedPaymentsAnalysis() {
        Map<String, Object> analysis = paymentAnalyticsService.getFailedPaymentsAnalysis();
        return ResponseEntity.ok(analysis);
    }

    @GetMapping("/refunds")
    @Operation(summary = "Get Refund Statistics", description = "Get refund statistics and trends")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Refund statistics")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<Map<String, Object>> getRefundStatistics() {
        Map<String, Object> stats = paymentAnalyticsService.getRefundStatistics();
        return ResponseEntity.ok(stats);
    }
}
