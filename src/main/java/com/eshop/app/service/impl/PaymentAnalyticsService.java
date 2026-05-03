package com.eshop.app.service.impl;

import com.eshop.app.entity.Payment;
import com.eshop.app.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Payment Analytics Service
 * Provides comprehensive analytics for payment performance, success rates, and financial metrics
 * Time Complexity: O(1) - O(n) depending on query type, optimized with database indexes
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentAnalyticsService {
    
    private final PaymentRepository paymentRepository;
    
    /**
     * Get payment dashboard analytics
     * Time Complexity: O(1) with database indexes
     */
    public Map<String, Object> getPaymentDashboard() {
        Map<String, Object> dashboard = new HashMap<>();
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime todayStart = now.withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime monthStart = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime yearStart = now.withDayOfYear(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        
        // Total payments and revenue
        dashboard.put("totalPayments", paymentRepository.count());
        dashboard.put("totalRevenue", getTotalRevenue());
        
        // Today's metrics
        dashboard.put("todayRevenue", calculateRevenueBetween(todayStart, now));
        dashboard.put("todayPayments", countPaymentsBetween(todayStart, now));
        
        // Monthly metrics
        dashboard.put("monthlyRevenue", calculateRevenueBetween(monthStart, now));
        dashboard.put("monthlyPayments", countPaymentsBetween(monthStart, now));
        
        // Yearly metrics
        dashboard.put("yearlyRevenue", calculateRevenueBetween(yearStart, now));
        dashboard.put("yearlyPayments", countPaymentsBetween(yearStart, now));
        
        // Success rates
        dashboard.put("overallSuccessRate", calculateOverallSuccessRate());
        dashboard.put("todaySuccessRate", calculateSuccessRateBetween(todayStart, now));
        
        // Gateway performance
        dashboard.put("gatewayStats", getGatewayStatistics());
        
        // Payment method distribution
        dashboard.put("paymentMethodStats", getPaymentMethodStatistics());
        
        // Recent trends (last 7 days)
        dashboard.put("weeklyTrend", getWeeklyTrend());
        
        return dashboard;
    }
    
    /**
     * Get gateway-wise statistics
     * Time Complexity: O(g) where g is number of gateways
     */
    public Map<String, Object> getGatewayStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        List<Object[]> gatewayStats = paymentRepository.getPaymentStatisticsByGateway();
        
        for (Object[] stat : gatewayStats) {
            Payment.PaymentGateway gateway = (Payment.PaymentGateway) stat[0];
            Long count = (Long) stat[1];
            Double avgAmount = (Double) stat[2];
            BigDecimal totalAmount = (BigDecimal) stat[3];
            
            Map<String, Object> gatewayStat = new HashMap<>();
            gatewayStat.put("count", count);
            gatewayStat.put("averageAmount", avgAmount != null ? BigDecimal.valueOf(avgAmount) : BigDecimal.ZERO);
            gatewayStat.put("totalAmount", totalAmount != null ? totalAmount : BigDecimal.ZERO);
            gatewayStat.put("successRate", calculateGatewaySuccessRate(gateway));
            
            stats.put(gateway.name(), gatewayStat);
        }
        
        return stats;
    }
    
    /**
     * Get payment method statistics
     * Time Complexity: O(m) where m is number of payment methods
     */
    public Map<String, Object> getPaymentMethodStatistics() {
        List<Object[]> methodStats = paymentRepository.getPaymentCountByMethod();
        Map<String, Object> stats = new HashMap<>();
        
        long totalPayments = methodStats.stream()
                .mapToLong(stat -> (Long) stat[1])
                .sum();
        
        for (Object[] stat : methodStats) {
            Payment.PaymentMethod method = (Payment.PaymentMethod) stat[0];
            Long count = (Long) stat[1];
            
            Map<String, Object> methodStat = new HashMap<>();
            methodStat.put("count", count);
            methodStat.put("percentage", totalPayments > 0 ? 
                BigDecimal.valueOf((double) count / totalPayments * 100)
                    .setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO);
            
            stats.put(method.name(), methodStat);
        }
        
        return stats;
    }
    
    /**
     * Get weekly payment trends (last 7 days)
     * Time Complexity: O(7) = O(1)
     */
    public List<Map<String, Object>> getWeeklyTrend() {
        List<Map<String, Object>> trend = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        
        for (int i = 6; i >= 0; i--) {
            LocalDateTime dayStart = now.minusDays(i).withHour(0).withMinute(0).withSecond(0).withNano(0);
            LocalDateTime dayEnd = dayStart.plusDays(1).minusNanos(1);
            
            Map<String, Object> dayStats = new HashMap<>();
            dayStats.put("date", dayStart.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            dayStats.put("revenue", calculateRevenueBetween(dayStart, dayEnd));
            dayStats.put("payments", countPaymentsBetween(dayStart, dayEnd));
            dayStats.put("successRate", calculateSuccessRateBetween(dayStart, dayEnd));
            
            trend.add(dayStats);
        }
        
        return trend;
    }
    
    /**
     * Get hourly payment trends for today
     * Time Complexity: O(24) = O(1)
     */
    public List<Map<String, Object>> getHourlyTrendToday() {
        List<Map<String, Object>> trend = new ArrayList<>();
        LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        
        for (int hour = 0; hour < 24; hour++) {
            LocalDateTime hourStart = todayStart.plusHours(hour);
            LocalDateTime hourEnd = hourStart.plusHours(1).minusNanos(1);
            
            Map<String, Object> hourStats = new HashMap<>();
            hourStats.put("hour", hour);
            hourStats.put("revenue", calculateRevenueBetween(hourStart, hourEnd));
            hourStats.put("payments", countPaymentsBetween(hourStart, hourEnd));
            
            trend.add(hourStats);
        }
        
        return trend;
    }
    
    /**
     * Get payment performance by country/region
     * Time Complexity: O(n) where n is number of payments
     */
    public Map<String, Object> getPaymentPerformanceByRegion() {
        Map<String, Object> regionStats = new HashMap<>();
        
        // This would require adding country/region fields to Payment entity
        // For now, we'll provide gateway-based regional analysis
        Map<String, Object> gatewayRegions = new HashMap<>();
        
        // Stripe - International
        gatewayRegions.put("International", getGatewayPerformance(Payment.PaymentGateway.STRIPE));
        
        // Indian gateways
        Map<String, Object> indiaStats = new HashMap<>();
        indiaStats.put("razorpay", getGatewayPerformance(Payment.PaymentGateway.RAZORPAY));
        indiaStats.put("payu", getGatewayPerformance(Payment.PaymentGateway.PAYU));
        indiaStats.put("cashfree", getGatewayPerformance(Payment.PaymentGateway.CASHFREE));
        gatewayRegions.put("India", indiaStats);
        
        regionStats.put("regions", gatewayRegions);
        
        return regionStats;
    }
    
    /**
     * Get failed payments analysis
     * Time Complexity: O(f) where f is number of failed payments
     */
    public Map<String, Object> getFailedPaymentsAnalysis() {
        Map<String, Object> analysis = new HashMap<>();
        
        LocalDateTime last7Days = LocalDateTime.now().minusDays(7);
        List<Payment> failedPayments = paymentRepository.findFailedPaymentsSince(
            Payment.PaymentStatus.FAILED, last7Days);
        
        // Group by failure reasons
        Map<String, Long> failureReasons = failedPayments.stream()
                .collect(Collectors.groupingBy(
                    p -> p.getResponseMessage() != null ? p.getResponseMessage() : "Unknown",
                    Collectors.counting()));
        
        // Group by gateway
        Map<Payment.PaymentGateway, Long> failuresByGateway = failedPayments.stream()
                .collect(Collectors.groupingBy(
                    Payment::getGateway,
                    Collectors.counting()));
        
        analysis.put("totalFailedPayments", failedPayments.size());
        analysis.put("failureReasons", failureReasons);
        analysis.put("failuresByGateway", failuresByGateway);
        analysis.put("period", "Last 7 days");
        
        return analysis;
    }
    
    /**
     * Get refund statistics
     * Time Complexity: O(r) where r is number of refunded payments
     */
    public Map<String, Object> getRefundStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        // This would require adding refund tracking to Payment entity
        // For now, we'll provide basic structure
        stats.put("totalRefunds", 0);
        stats.put("totalRefundAmount", BigDecimal.ZERO);
        stats.put("refundRate", BigDecimal.ZERO);
        stats.put("averageRefundAmount", BigDecimal.ZERO);
        
        return stats;
    }
    
    // Helper Methods
    
    private BigDecimal getTotalRevenue() {
        return paymentRepository.calculateRevenueBetween(
            LocalDateTime.of(2020, 1, 1, 0, 0),
            LocalDateTime.now());
    }
    
    private BigDecimal calculateRevenueBetween(LocalDateTime start, LocalDateTime end) {
        return paymentRepository.calculateRevenueBetween(start, end);
    }
    
    private long countPaymentsBetween(LocalDateTime start, LocalDateTime end) {
        return paymentRepository.findPaymentsBetweenDates(start, end).size();
    }
    
    private BigDecimal calculateOverallSuccessRate() {
        long totalPayments = paymentRepository.count();
        if (totalPayments == 0) return BigDecimal.ZERO;
        
        long successfulPayments = paymentRepository.findByStatus(
            Payment.PaymentStatus.COMPLETED, null).getTotalElements();
        
        return BigDecimal.valueOf((double) successfulPayments / totalPayments * 100)
                .setScale(2, RoundingMode.HALF_UP);
    }
    
    private BigDecimal calculateSuccessRateBetween(LocalDateTime start, LocalDateTime end) {
        List<Payment> allPayments = paymentRepository.findPaymentsBetweenDates(start, end);
        if (allPayments.isEmpty()) return BigDecimal.ZERO;
        
        long successfulCount = allPayments.stream()
                .mapToLong(p -> p.getStatus() == Payment.PaymentStatus.COMPLETED ? 1 : 0)
                .sum();
        
        return BigDecimal.valueOf((double) successfulCount / allPayments.size() * 100)
                .setScale(2, RoundingMode.HALF_UP);
    }
    
    private BigDecimal calculateGatewaySuccessRate(Payment.PaymentGateway gateway) {
        long totalGatewayPayments = paymentRepository.findByGateway(gateway, null).getTotalElements();
        if (totalGatewayPayments == 0) return BigDecimal.ZERO;
        
        long successfulGatewayPayments = paymentRepository.findByGatewayAndStatusSince(
            gateway, Payment.PaymentStatus.COMPLETED, LocalDateTime.of(2020, 1, 1, 0, 0)).size();
        
        return BigDecimal.valueOf((double) successfulGatewayPayments / totalGatewayPayments * 100)
                .setScale(2, RoundingMode.HALF_UP);
    }
    
    private Map<String, Object> getGatewayPerformance(Payment.PaymentGateway gateway) {
        Map<String, Object> performance = new HashMap<>();
        
        BigDecimal revenue = paymentRepository.calculateRevenueByGateway(gateway);
        BigDecimal successRate = calculateGatewaySuccessRate(gateway);
        long totalPayments = paymentRepository.findByGateway(gateway, null).getTotalElements();
        
        performance.put("revenue", revenue);
        performance.put("successRate", successRate);
        performance.put("totalPayments", totalPayments);
        performance.put("averageAmount", totalPayments > 0 ? 
            revenue.divide(BigDecimal.valueOf(totalPayments), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO);
        
        return performance;
    }
}