package com.eshop.app.catalog.application.service.impl;

import com.eshop.app.catalog.application.port.in.HomeUseCase;
import com.eshop.app.catalog.domain.repository.ProductRepository;
import com.eshop.app.order.domain.entity.Order;
import com.eshop.app.order.domain.repository.OrderRepository;
import com.eshop.app.core.api.response.HomeResponse;
import com.eshop.app.store.domain.repository.StoreRepository;
import com.eshop.app.user.domain.entity.User;
import com.eshop.app.user.shared.domain.enums.UserRole;
import com.eshop.app.user.domain.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class HomeUseCaseImpl implements HomeUseCase {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final StoreRepository storeRepository;
    private final OrderRepository orderRepository;

    @Override
    @Transactional(readOnly = true)
    public HomeResponse getHomePageData(Authentication authentication) {
        User user = getUserFromAuthentication(authentication);
        return getHomePageDataForUser(user);
    }

    private User getUserFromAuthentication(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        if (authentication
                .getPrincipal() instanceof com.eshop.app.core.infrastructure.config.security.oauth.PrincipalDetails details) {
            Long userId = details.getId();
            return userRepository.findById(userId).orElse(null);
        }

        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public HomeResponse getHomePageDataForUser(User user) {
        if (user == null) {
            return HomeResponse.forGuest();
        }

        UserRole role = user.getRole();
        String userName = user.getUserProfile() != null
                ? user.getUserProfile().getFirstName() + " " + user.getUserProfile().getLastName()
                : user.getEmail();

        log.info("Generating home page data for user: {} with role: {}", userName, role);

        return switch (role) {
            case ADMIN -> HomeResponse.forAdmin(userName, getAdminDashboardData(user));
            case SELLER -> HomeResponse.forSeller(userName, getSellerDashboardData(user));
            case CUSTOMER -> HomeResponse.forCustomer(userName, getCustomerDashboardData(user));
            case DELIVERY_AGENT -> HomeResponse.forDeliveryAgent(userName, getDeliveryAgentDashboardData(user));
            default -> HomeResponse.forGuest();
        };
    }

    private Map<String, Object> getAdminDashboardData(User user) {
        long totalUsers = userRepository != null ? userRepository.count() : 0L;
        long totalProducts = productRepository != null ? productRepository.count() : 0L;
        long totalShops = storeRepository != null ? storeRepository.count() : 0L;
        long totalOrders = orderRepository != null ? orderRepository.count() : 0L;

        LocalDateTime startOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime endOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);
        long todayOrders = orderRepository != null ? orderRepository.countOrdersBetweenDates(startOfDay, endOfDay) : 0L;

        Map<String, Object> data = new HashMap<>();
        data.put("totalUsers", totalUsers);
        data.put("totalProducts", totalProducts);
        data.put("totalShops", totalShops);
        data.put("totalOrders", totalOrders);
        data.put("todayOrders", todayOrders);
        data.put("systemHealth", "Operational");

        return data;
    }

    private Map<String, Object> getSellerDashboardData(User user) {
        Map<String, Object> data = new HashMap<>();

        if (user.getStore() != null) {
            Long storeId = user.getStore().getId();
            long totalProducts = productRepository != null
                    ? productRepository.findByStoreId(storeId, org.springframework.data.domain.Pageable.unpaged())
                            .getTotalElements()
                    : 0L;
            long totalOrders = orderRepository != null
                    ? orderRepository.findByStoreId(storeId, org.springframework.data.domain.Pageable.unpaged())
                            .getTotalElements()
                    : 0L;

            long pendingOrders = 0L;
            if (orderRepository != null) {
                pendingOrders = orderRepository
                        .findByOrderStatus(Order.OrderStatus.PLACED, org.springframework.data.domain.Pageable.unpaged())
                        .stream()
                        .filter(order -> order.getItems().stream()
                                .anyMatch(item -> item.getProduct().getStore().getId().equals(storeId)))
                        .count();
            }

            LocalDateTime startOfMonth = LocalDateTime.of(LocalDate.now().withDayOfMonth(1), LocalTime.MIN);
            LocalDateTime endOfMonth = LocalDateTime.now();
            BigDecimal monthlySales = orderRepository != null
                    ? orderRepository.sumRevenueBetweenDates(startOfMonth, endOfMonth)
                    : null;

            data.put("storeName", user.getStore().getStoreName());
            data.put("totalProducts", totalProducts);
            data.put("totalOrders", totalOrders);
            data.put("pendingOrders", pendingOrders);
            data.put("monthlySales", monthlySales != null ? "â‚¹" + monthlySales : "â‚¹0");
            data.put("shopStatus", user.getStore().getActive() ? "Active" : "Inactive");
        } else {
            data.put("storeName", "No Store");
            data.put("totalProducts", 0);
            data.put("totalOrders", 0);
            data.put("pendingOrders", 0);
            data.put("monthlySales", "â‚¹0");
            data.put("shopStatus", "Not Created");
        }

        return data;
    }

    private Map<String, Object> getCustomerDashboardData(User user) {
        Long userId = user.getId();
        long totalOrders = orderRepository != null ? orderRepository.countByCustomerId(userId) : 0L;
        BigDecimal totalSpent = orderRepository != null ? orderRepository.sumTotalAmountByCustomerId(userId) : null;

        int oartItemsCount = 0;
        if (user.getCart() != null && user.getCart().getItems() != null) {
            oartItemsCount = user.getCart().getItems().size();
        }

        long pendingOrders = 0L;
        if (orderRepository != null) {
            pendingOrders = orderRepository.findByCustomerId(userId, org.springframework.data.domain.Pageable.unpaged())
                    .stream()
                    .filter(order -> order.getOrderStatus() == Order.OrderStatus.PLACED ||
                            order.getOrderStatus() == Order.OrderStatus.CONFIRMED ||
                            order.getOrderStatus() == Order.OrderStatus.PACKED)
                    .count();
        }

        Map<String, Object> data = new HashMap<>();
        data.put("totalOrders", totalOrders);
        data.put("pendingOrders", pendingOrders);
        data.put("oartItems", oartItemsCount);
        data.put("totalSpent", totalSpent != null ? "â‚¹" + totalSpent : "â‚¹0");
        data.put("aocountStatus", "Active");

        return data;
    }

    private Map<String, Object> getDeliveryAgentDashboardData(User user) {
        Long agentId = user.getId();
        long totalAssignedDeliveries = orderRepository != null
                ? orderRepository.findByDeliveryAgentId(agentId, org.springframework.data.domain.Pageable.unpaged())
                        .getTotalElements()
                : 0L;

        LocalDateTime startOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime endOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);

        long completedToday = 0L;
        if (orderRepository != null) {
            completedToday = orderRepository
                    .findByDeliveryAgentId(agentId, org.springframework.data.domain.Pageable.unpaged())
                    .stream()
                    .filter(order -> order.getOrderStatus() == Order.OrderStatus.DELIVERED &&
                            order.getUpdatedAt() != null &&
                            order.getUpdatedAt().isAfter(startOfDay) &&
                            order.getUpdatedAt().isBefore(endOfDay))
                    .count();
        }

        long pendingPiokups = 0L;
        if (orderRepository != null) {
            pendingPiokups = orderRepository
                    .findByDeliveryAgentId(agentId, org.springframework.data.domain.Pageable.unpaged())
                    .stream()
                    .filter(order -> order.getOrderStatus() == Order.OrderStatus.SHIPPED)
                    .count();
        }

        Map<String, Object> data = new HashMap<>();
        data.put("assignedDeliveries", totalAssignedDeliveries);
        data.put("completedToday", completedToday);
        data.put("pendingPiokups", pendingPiokups);
        data.put("todayEarnings", "â‚¹0");
        data.put("averageRating", "N/A");

        return data;
    }
}

