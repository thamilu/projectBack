package com.eshop.app.order.application.service.impl;

import com.eshop.app.core.util.DateTimeUtils;
import com.eshop.app.order.application.port.in.OrderAnalyticsUseCase;
import com.eshop.app.order.domain.entity.Order;
import com.eshop.app.order.domain.repository.OrderRepository;
import com.eshop.app.seller.api.response.SellerAggregationMetricsDTO;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class OrderAnalyticsUseCaseImpl implements OrderAnalyticsUseCase {

    private final OrderRepository orderRepository;

    @Override
    public long getTotalOrderCount() {
        return orderRepository.count();
    }

    @Override
    public long getPendingOrderCount() {
        return orderRepository.countByOrderStatus(Order.OrderStatus.PLACED);
    }

    @Override
    public long getTodayOrderCount() {
        return orderRepository.countOrdersBetweenDates(DateTimeUtils.startOfDay(), LocalDateTime.now());
    }

    @Override
    public BigDecimal getTotalRevenue() {
        return orderRepository.sumTotalRevenue();
    }

    @Override
    public BigDecimal getMonthlyRevenue() {
        BigDecimal total = orderRepository.sumRevenueBetweenDates(DateTimeUtils.startOfMonth(), LocalDateTime.now());
        return total != null ? total : BigDecimal.ZERO;
    }

    @Override
    public BigDecimal getTodayRevenueBySellerId(Long sellerId) {
        return orderRepository.sumRevenueBySellerIdBetweenDates(sellerId, DateTimeUtils.startOfDay(), LocalDateTime.now());
    }

    @Override
    public BigDecimal getWeeklyRevenueBySellerId(Long sellerId) {
        return orderRepository.sumRevenueBySellerIdBetweenDates(sellerId, DateTimeUtils.startOfWeek(), LocalDateTime.now());
    }

    @Override
    public BigDecimal getMonthlyRevenueBySellerId(Long sellerId) {
        return orderRepository.sumRevenueBySellerIdBetweenDates(sellerId, DateTimeUtils.startOfMonth(), LocalDateTime.now());
    }

    @Override
    public BigDecimal getTotalRevenueBySellerId(Long sellerId) {
        return orderRepository.sumRevenueBySellerIdBetweenDates(sellerId, LocalDateTime.of(1970, 1, 1, 0, 0), LocalDateTime.now());
    }

    @Override
    public long getNewOrderCountBySellerId(Long sellerId) {
        return orderRepository.countByStoreSellerIdAndOrderStatus(sellerId, Order.OrderStatus.PLACED);
    }

    @Override
    public long getProcessingOrderCountBySellerId(Long sellerId) {
        return orderRepository.countByStoreSellerIdAndOrderStatus(sellerId, Order.OrderStatus.CONFIRMED);
    }

    @Override
    public long getShippedOrderCountBySellerId(Long sellerId) {
        return orderRepository.countByStoreSellerIdAndOrderStatus(sellerId, Order.OrderStatus.SHIPPED);
    }

    @Override
    public long getCompletedOrderCountBySellerId(Long sellerId) {
        return orderRepository.countByStoreSellerIdAndOrderStatus(sellerId, Order.OrderStatus.DELIVERED);
    }

    @Override
    public SellerAggregationMetricsDTO getSellerAggregationMetrics(Long sellerId) {
        return orderRepository.getSellerAggregationMetrics(
                sellerId,
                DateTimeUtils.startOfDay(),
                DateTimeUtils.startOfWeek(),
                DateTimeUtils.startOfMonth());
    }

    @Override
    public List<Map<String, Object>> getRecentOrdersBySellerId(Long sellerId, int limit) {
        return orderRepository
                .findRecentOrdersBySellerId(sellerId, org.springframework.data.domain.PageRequest.of(0, limit))
                .stream()
                .map(o -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("orderNumber", o.getOrderNumber());
                    map.put("status", o.getOrderStatus().toString());
                    map.put("totalAmount", o.getTotalAmount());
                    map.put("createdAt", o.getCreatedAt());
                    return map;
                })
                .toList();
    }

    @Override
    public Map<String, BigDecimal> getRevenueBreakdownBySellerId(Long sellerId) {
        Map<String, BigDecimal> breakdown = new HashMap<>();
        breakdown.put("totalRevenue", getTotalRevenueBySellerId(sellerId));
        breakdown.put("monthlyRevenue", getMonthlyRevenueBySellerId(sellerId));
        breakdown.put("weeklyRevenue", getWeeklyRevenueBySellerId(sellerId));
        breakdown.put("todayRevenue", getTodayRevenueBySellerId(sellerId));
        return breakdown;
    }

    @Override
    public long getOrderCountByCustomerId(Long customerId) {
        return orderRepository.countByCustomerId(customerId);
    }

    @Override
    public List<Map<String, Object>> getRecentOrdersByCustomerId(Long customerId, int limit) {
        return orderRepository.findByCustomerId(customerId, org.springframework.data.domain.PageRequest.of(0, limit))
                .stream()
                .map(o -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("orderNumber", o.getOrderNumber());
                    map.put("status", o.getOrderStatus().toString());
                    map.put("totalAmount", o.getTotalAmount());
                    map.put("createdAt", o.getCreatedAt());
                    return map;
                })
                .toList();
    }

    @Override
    public BigDecimal getTotalSpentByCustomerId(Long customerId) {
        BigDecimal total = orderRepository.sumTotalAmountByCustomerId(customerId);
        return total != null ? total : BigDecimal.ZERO;
    }

    @Override
    public BigDecimal getAverageOrderValueByCustomerId(Long customerId) {
        long count = orderRepository.countByCustomerId(customerId);
        if (count == 0) return BigDecimal.ZERO;
        BigDecimal total = orderRepository.sumTotalAmountByCustomerId(customerId);
        return total != null ? total.divide(BigDecimal.valueOf(count), 2, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO;
    }

    @Override
    public long getPendingDeliveriesByAgentId(Long agentId) {
        return orderRepository.countByDeliveryAgentIdAndOrderStatus(agentId, Order.OrderStatus.PLACED);
    }

    @Override
    public long getTodayDeliveriesByAgentId(Long agentId) {
        return orderRepository.countByDeliveryAgentIdAndCreatedAtAfter(agentId, DateTimeUtils.startOfDay());
    }

    @Override
    public long getInTransitOrdersByAgentId(Long agentId) {
        return orderRepository.countByDeliveryAgentIdAndOrderStatus(agentId, Order.OrderStatus.SHIPPED);
    }

    @Override
    public long getCompletedDeliveriesTodayByAgentId(Long agentId) {
        return orderRepository.countByDeliveryAgentIdAndOrderStatusAndCreatedAtAfter(agentId, Order.OrderStatus.DELIVERED, DateTimeUtils.startOfDay());
    }

    @Override
    public long getCompletedDeliveriesThisWeekByAgentId(Long agentId) {
        return orderRepository.countByDeliveryAgentIdAndOrderStatusAndCreatedAtAfter(agentId, Order.OrderStatus.DELIVERED, DateTimeUtils.startOfWeek());
    }

    @Override
    public long getCompletedDeliveriesThisMonthByAgentId(Long agentId) {
        return orderRepository.countByDeliveryAgentIdAndOrderStatusAndCreatedAtAfter(agentId, Order.OrderStatus.DELIVERED, DateTimeUtils.startOfMonth());
    }

    @Override
    public double getDeliverySuccessRateByAgentId(Long agentId) {
        long total = orderRepository.countByDeliveryAgentId(agentId);
        if (total == 0) return 0.0;
        long successful = orderRepository.countByDeliveryAgentIdAndOrderStatus(agentId, Order.OrderStatus.DELIVERED);
        return (double) successful / total * 100;
    }

    @Override
    public List<Map<String, Object>> getRecentDeliveriesByAgentId(Long agentId, int limit) {
        return orderRepository.findByDeliveryAgentIdOrderByCreatedAtDesc(agentId, org.springframework.data.domain.PageRequest.of(0, limit))
                .stream()
                .map(o -> Map.<String, Object>of(
                        "orderNumber", o.getOrderNumber(),
                        "status", o.getOrderStatus().toString(),
                        "totalAmount", o.getTotalAmount(),
                        "createdAt", o.getCreatedAt()))
                .toList();
    }

    @Override
    public long getUrgentDeliveriesByAgentId(Long agentId) {
        // [HARDEN] Urgent deliveries are currently defined as PLACED orders assigned to the agent
        return orderRepository.countByDeliveryAgentIdAndOrderStatus(agentId, Order.OrderStatus.PLACED);
    }

    @Override
    public double getAverageDeliveryTimeByAgentId(Long agentId) {
        // [HARDEN] Implementation of actual delivery time calculation requires audit trail or specific timestamp fields
        return 0.0; // Placeholder
    }

    @Override
    public double getCustomerRatingByAgentId(Long agentId) {
        // [HARDEN] Customer rating for delivery agents requires a Review system linked to delivery
        return 0.0; // Placeholder
    }
}
