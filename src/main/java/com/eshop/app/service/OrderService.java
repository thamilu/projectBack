package com.eshop.app.service;

import com.eshop.app.dto.request.CheckoutRequest;
import com.eshop.app.dto.request.OrderCreateRequest;
import com.eshop.app.dto.response.OrderResponse;
import com.eshop.app.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;

public interface OrderService {
    OrderResponse createOrder(OrderCreateRequest request);

    OrderResponse getOrderById(Long id);

    OrderResponse getOrderByOrderNumber(String orderNumber);

    PageResponse<OrderResponse> getMyOrders(Pageable pageable);

    PageResponse<OrderResponse> getAllOrders(Pageable pageable);

    PageResponse<OrderResponse> getOrdersByStatus(String status, Pageable pageable);

    PageResponse<OrderResponse> getOrdersByStore(Long storeId, Pageable pageable);

    OrderResponse updateOrderStatus(Long orderId, String status);

    OrderResponse updatePaymentStatus(Long orderId, String status);

    OrderResponse assignDeliveryAgent(Long orderId, Long agentId);

    PageResponse<OrderResponse> getDeliveryAgentOrders(Pageable pageable);

    PageResponse<OrderResponse> getSellerOrders(Pageable pageable);

    // Dashboard Analytics Methods
    long getTotalOrderCount();

    long getPendingOrderCount();

    long getTodayOrderCount();

    java.math.BigDecimal getTotalRevenue();

    java.math.BigDecimal getMonthlyRevenue();

    // Seller-specific dashboard methods
    java.math.BigDecimal getTodayRevenueBySellerId(Long sellerId);

    java.math.BigDecimal getWeeklyRevenueBySellerId(Long sellerId);

    java.math.BigDecimal getMonthlyRevenueBySellerId(Long sellerId);

    java.math.BigDecimal getTotalRevenueBySellerId(Long sellerId);

    long getNewOrderCountBySellerId(Long sellerId);

    long getProcessingOrderCountBySellerId(Long sellerId);

    long getShippedOrderCountBySellerId(Long sellerId);

    long getCompletedOrderCountBySellerId(Long sellerId);

    java.util.List<java.util.Map<String, Object>> getRecentOrdersBySellerId(Long sellerId, int limit);

    java.util.List<java.util.Map<String, Object>> getDailySalesData();

    java.util.List<java.util.Map<String, Object>> getMonthlySalesData();

    java.util.Map<String, java.math.BigDecimal> getRevenueByCategory();

    java.util.List<java.util.Map<String, Object>> getSalesTrendBySellerId(Long sellerId);

    java.util.Map<String, Object> getCustomerDemographicsBySellerId(Long sellerId);

    java.util.Map<String, java.math.BigDecimal> getRevenueBreakdownBySellerId(Long sellerId);

    // Customer-specific dashboard methods
    long getOrderCountByCustomerId(Long customerId);

    java.util.List<java.util.Map<String, Object>> getRecentOrdersByCustomerId(Long customerId, int limit);

    java.math.BigDecimal getTotalSpentByCustomerId(Long customerId);

    java.math.BigDecimal getAverageOrderValueByCustomerId(Long customerId);

    // Delivery Agent-specific dashboard methods
    long getPendingDeliveriesByAgentId(Long agentId);

    long getTodayDeliveriesByAgentId(Long agentId);

    long getInTransitOrdersByAgentId(Long agentId);

    long getUrgentDeliveriesByAgentId(Long agentId);

    long getCompletedDeliveriesTodayByAgentId(Long agentId);

    long getCompletedDeliveriesThisWeekByAgentId(Long agentId);

    long getCompletedDeliveriesThisMonthByAgentId(Long agentId);

    double getAverageDeliveryTimeByAgentId(Long agentId);

    double getDeliverySuccessRateByAgentId(Long agentId);

    double getCustomerRatingByAgentId(Long agentId);

    java.util.List<java.util.Map<String, Object>> getRecentDeliveriesByAgentId(Long agentId, int limit);

    // Enhanced checkout methods for comprehensive cart-to-order conversion

    /**
     * Processes checkout for anonymous cart without user authentication.
     * Converts cart items to order with comprehensive validation and stock
     * management.
     * 
     * Features:
     * - Stock validation before order creation
     * - Automatic inventory deduction
     * - Order total calculation (subtotal + tax + shipping)
     * - Cart clearing after successful checkout
     * - Support for guest user orders
     * 
     * @param cartCode Unique cart identifier (UUID format)
     * @param request  Checkout details (shipping, billing, payment info)
     * @return OrderResponse with created order details
     * 
     * @throws ResourceNotFoundException  if cart not found
     * @throws EmptyCartException         if cart has no items
     * @throws InsufficientStockException if any product lacks stock
     * @throws ValidationException        if checkout request is invalid
     * 
     *                                    Time Complexity: O(n) where n is number of
     *                                    cart items
     *                                    Space Complexity: O(n) for order item
     *                                    creation
     * 
     *                                    Business Rules:
     *                                    - Cart must have at least one item
     *                                    - All products must have sufficient stock
     *                                    - Shipping address is mandatory
     *                                    - Order number is auto-generated
     *                                    - Initial status: PLACED, Payment status:
     *                                    PENDING
     */
    OrderResponse checkoutAnonymousCart(String cartCode, CheckoutRequest request);

    /**
     * Processes checkout for authenticated user cart with ownership validation.
     * Provides same functionality as anonymous checkout with user linking.
     * 
     * Features:
     * - User ownership verification
     * - Order linked to authenticated user account
     * - Access to user's order history
     * - Enhanced security validation
     * - All anonymous checkout features
     * 
     * @param cartCode Unique cart identifier
     * @param request  Checkout details (shipping, billing, payment info)
     * @return OrderResponse with created order details
     * 
     * @throws ResourceNotFoundException  if cart not found or access denied
     * @throws EmptyCartException         if cart has no items
     * @throws InsufficientStockException if any product lacks stock
     * @throws UnauthorizedException      if user not authenticated
     * @throws ValidationException        if checkout request is invalid
     * 
     *                                    Time Complexity: O(n) where n is number of
     *                                    cart items
     *                                    Space Complexity: O(n) for order item
     *                                    creation
     * 
     *                                    Security Rules:
     *                                    - Cart must belong to authenticated user
     *                                    - JWT token validation required
     *                                    - User must have CUSTOMER role or higher
     */
    OrderResponse checkoutAuthenticatedCart(String cartCode, CheckoutRequest request);
}
