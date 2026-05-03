package com.eshop.app.service.impl;

import com.eshop.app.dto.request.CheckoutRequest;
import com.eshop.app.enums.UserRole;
import com.eshop.app.dto.request.OrderCreateRequest;
import com.eshop.app.dto.response.OrderResponse;
import com.eshop.app.dto.response.PageResponse;
import com.eshop.app.entity.*;
import com.eshop.app.exception.EmptyCartException;
import com.eshop.app.exception.InsufficientStockException;
import com.eshop.app.exception.ResourceNotFoundException;
import com.eshop.app.mapper.EntityMapper;
import com.eshop.app.repository.*;
import com.eshop.app.service.OrderService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;

@Service
@Transactional
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    @SuppressWarnings("unused") // Reserved for future order item operations
    private final OrderItemRepository orderItemRepository;
    private final EntityMapper entityMapper;

    @Value("${app.business.default-tax-rate:0.10}")
    private BigDecimal defaultTaxRate;

    public OrderServiceImpl(OrderRepository orderRepository,
            CartRepository cartRepository,
            ProductRepository productRepository,
            UserRepository userRepository,
            OrderItemRepository orderItemRepository,
            EntityMapper entityMapper) {
        this.orderRepository = orderRepository;
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.orderItemRepository = orderItemRepository;
        this.entityMapper = entityMapper;
    }

    private Long getCurrentUserId() {
        return com.eshop.app.util.SecurityUtils.getAuthenticatedUserId();
    }

    private String generateOrderNumber() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return "ORD-" + timestamp + "-" + (int) (Math.random() * 1000);
    }

    @Override
    public OrderResponse createOrder(OrderCreateRequest request) {
        Long userId = getCurrentUserId();

        User customer = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new EmptyCartException("Cannot create order from empty cart");
        }

        // Validate stock for all items
        for (CartItem cartItem : cart.getItems()) {
            Product product = cartItem.getProduct();
            if (product.getStockQuantity() < cartItem.getQuantity()) {
                throw new InsufficientStockException("Insufficient stock for product: " + product.getName());
            }
        }

        // Create order
        Order order = Order.builder()
                .orderNumber(generateOrderNumber())
                .customer(customer)
                .shippingAddress(request.getShippingAddress())
                .billingAddress(request.getBillingAddress() != null ? request.getBillingAddress()
                        : request.getShippingAddress())
                .phone(request.getPhone())
                .notes(request.getNotes())
                .orderStatus(Order.OrderStatus.PLACED)
                .paymentStatus(Order.PaymentStatus.PENDING)
                .build();

        // Create order items and calculate totals
        Set<OrderItem> orderItems = new HashSet<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<Product> productsToUpdate = new ArrayList<>();

        for (CartItem cartItem : cart.getItems()) {
            Product product = cartItem.getProduct();

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .quantity(cartItem.getQuantity())
                    .price(cartItem.getPrice())
                    .discountAmount(BigDecimal.ZERO)
                    .build();

            orderItems.add(orderItem);
            totalAmount = totalAmount.add(orderItem.getPrice().multiply(BigDecimal.valueOf(orderItem.getQuantity())));

            // Update product stock (batch later)
            product.setStockQuantity(product.getStockQuantity() - cartItem.getQuantity());
            productsToUpdate.add(product);
        }

        // Batch save product stock updates (N+1 Writes Fix)
        if (!productsToUpdate.isEmpty()) {
            productRepository.saveAll(productsToUpdate);
        }

        order.setItems(orderItems);
        order.setTotalAmount(totalAmount);
        order.setShippingAmount(BigDecimal.valueOf(10.00)); // Fixed shipping
        order.setTaxAmount(totalAmount.multiply(defaultTaxRate)); // Dynamic tax based on property

        order = orderRepository.save(order);

        // Clear cart
        cart.getItems().clear();
        cart.calculateTotalAmount();
        cartRepository.save(cart);

        return entityMapper.toOrderResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));
        return entityMapper.toOrderResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderByOrderNumber(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with number: " + orderNumber));
        return entityMapper.toOrderResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getMyOrders(Pageable pageable) {
        Long userId = getCurrentUserId();
        Page<Order> orderPage = orderRepository.findByCustomerId(userId, pageable);
        return PageResponse.of(orderPage, entityMapper::toOrderResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getAllOrders(Pageable pageable) {
        Page<Order> orderPage = orderRepository.findAll(pageable);
        return PageResponse.of(orderPage, entityMapper::toOrderResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getOrdersByStatus(String status, Pageable pageable) {
        Order.OrderStatus orderStatus = Order.OrderStatus.valueOf(status.toUpperCase());
        Page<Order> orderPage = orderRepository.findByOrderStatus(orderStatus, pageable);
        return PageResponse.of(orderPage, entityMapper::toOrderResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getOrdersByStore(Long storeId, Pageable pageable) {
        Page<Order> orderPage = orderRepository.findByStoreId(storeId, pageable);
        return PageResponse.of(orderPage, entityMapper::toOrderResponse);
    }

    @Override
    public OrderResponse updateOrderStatus(Long orderId, String status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        Order.OrderStatus orderStatus = Order.OrderStatus.valueOf(status.toUpperCase());
        order.setOrderStatus(orderStatus);
        order = orderRepository.save(order);

        return entityMapper.toOrderResponse(order);
    }

    @Override
    public OrderResponse updatePaymentStatus(Long orderId, String status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        Order.PaymentStatus paymentStatus = Order.PaymentStatus.valueOf(status.toUpperCase());
        order.setPaymentStatus(paymentStatus);
        order = orderRepository.save(order);

        return entityMapper.toOrderResponse(order);
    }

    @Override
    public OrderResponse assignDeliveryAgent(Long orderId, Long agentId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        User agent = userRepository.findById(agentId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery agent not found"));

        if (agent.getRole() != UserRole.DELIVERY_AGENT) {
            throw new IllegalArgumentException("User is not a delivery agent");
        }

        order.setDeliveryAgent(agent);
        order = orderRepository.save(order);

        return entityMapper.toOrderResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getDeliveryAgentOrders(Pageable pageable) {
        Long agentId = getCurrentUserId();
        Page<Order> orderPage = orderRepository.findByDeliveryAgentId(agentId, pageable);
        return PageResponse.of(orderPage, entityMapper::toOrderResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getSellerOrders(Pageable pageable) {
        Long sellerId = getCurrentUserId();
        Page<Order> orderPage = orderRepository.findByStoreSellerId(sellerId, pageable);
        return PageResponse.of(orderPage, entityMapper::toOrderResponse);
    }

    @Override
    public OrderResponse checkoutAnonymousCart(String cartCode, CheckoutRequest request) {
        // Find cart by code (for anonymous users)
        Cart cart = cartRepository.findByCartCode(cartCode)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found with code: " + cartCode));

        if (cart.getItems().isEmpty()) {
            throw new EmptyCartException("Cannot checkout empty cart");
        }

        return processCheckout(cart, request, null);
    }

    @Override
    public OrderResponse checkoutAuthenticatedCart(String cartCode, CheckoutRequest request) {
        // Find cart by code and verify it belongs to current user
        Cart cart = cartRepository.findByCartCode(cartCode)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found with code: " + cartCode));

        Long currentUserId = getCurrentUserId();
        if (cart.getUser() == null || !cart.getUser().getId().equals(currentUserId)) {
            throw new ResourceNotFoundException("Cart not found or access denied");
        }

        if (cart.getItems().isEmpty()) {
            throw new EmptyCartException("Cannot checkout empty cart");
        }

        return processCheckout(cart, request, currentUserId);
    }

    private OrderResponse processCheckout(Cart cart, CheckoutRequest request, Long userId) {
        // Validate stock availability
        for (CartItem item : cart.getItems()) {
            Product product = item.getProduct();
            if (product.getStockQuantity() < item.getQuantity()) {
                throw new InsufficientStockException(
                        "Insufficient stock for product: " + product.getName() +
                                ". Available: " + product.getStockQuantity() +
                                ", Required: " + item.getQuantity());
            }
        }

        // Calculate totals (using basic calculation since CheckoutRequest doesn't have
        // tax/shipping fields)
        BigDecimal subtotal = cart.getItems().stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // For now, use zero for tax and shipping - can be enhanced later
        BigDecimal shippingAmount = BigDecimal.ZERO;
        BigDecimal taxAmount = BigDecimal.ZERO;
        BigDecimal totalAmount = subtotal.add(shippingAmount).add(taxAmount);

        // Create order
        Order order = new Order();
        order.setOrderNumber(generateOrderNumber());
        order.setTotalAmount(totalAmount);
        order.setTaxAmount(taxAmount);
        order.setShippingAmount(shippingAmount);
        order.setOrderStatus(Order.OrderStatus.PLACED);
        order.setPaymentStatus(Order.PaymentStatus.PENDING);
        order.setShippingAddress(request.getShippingAddress());
        order.setBillingAddress(request.getBillingAddress());
        order.setPhone(request.getPhone());
        order.setNotes(request.getNotes());

        // Set customer if authenticated
        if (userId != null) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            order.setCustomer(user);
        }

        // Create order items and update stock
        Set<OrderItem> orderItems = new HashSet<>();
        List<Product> productsToUpdate = new ArrayList<>();
        for (CartItem cartItem : cart.getItems()) {
            Product product = cartItem.getProduct();

            // Update product stock (batch later)
            product.setStockQuantity(product.getStockQuantity() - cartItem.getQuantity());
            productsToUpdate.add(product);

            // Create order item
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setPrice(product.getPrice());
            orderItem.setSubtotal(product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));

            orderItems.add(orderItem);
        }

        // Batch save product stock updates (N+1 Writes Fix)
        if (!productsToUpdate.isEmpty()) {
            productRepository.saveAll(productsToUpdate);
        }

        order.setItems(orderItems);
        Order savedOrder = orderRepository.save(order);

        // Clear the cart after successful checkout
        cart.getItems().clear();
        cartRepository.save(cart);

        return entityMapper.toOrderResponse(savedOrder);
    }

    // Dashboard Analytics Methods Implementation
    @Override
    @Transactional(readOnly = true)
    public long getTotalOrderCount() {
        return orderRepository.count();
    }

    @Override
    @Transactional(readOnly = true)
    public long getPendingOrderCount() {
        return orderRepository.countByOrderStatus(Order.OrderStatus.PLACED);
    }

    @Override
    @Transactional(readOnly = true)
    public long getTodayOrderCount() {
        return orderRepository.countOrdersBetweenDates(com.eshop.app.util.DateTimeUtils.startOfDay(),
                java.time.LocalDateTime.now());
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getTotalRevenue() {
        // Use the simpler query that doesn't require date range
        return orderRepository.sumTotalRevenue();
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getMonthlyRevenue() {
        BigDecimal total = orderRepository.sumRevenueBetweenDates(
                com.eshop.app.util.DateTimeUtils.startOfMonth(), java.time.LocalDateTime.now());
        return total != null ? total : BigDecimal.ZERO;
    }

    // Seller-specific dashboard methods implementation
    @Override
    @Transactional(readOnly = true)
    public BigDecimal getTodayRevenueBySellerId(Long sellerId) {
        return orderRepository.sumRevenueBySellerIdBetweenDates(sellerId,
                com.eshop.app.util.DateTimeUtils.startOfDay(), java.time.LocalDateTime.now());
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getWeeklyRevenueBySellerId(Long sellerId) {
        return orderRepository.sumRevenueBySellerIdBetweenDates(sellerId,
                com.eshop.app.util.DateTimeUtils.startOfWeek(), java.time.LocalDateTime.now());
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getMonthlyRevenueBySellerId(Long sellerId) {
        return orderRepository.sumRevenueBySellerIdBetweenDates(sellerId,
                com.eshop.app.util.DateTimeUtils.startOfMonth(), java.time.LocalDateTime.now());
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getTotalRevenueBySellerId(Long sellerId) {
        return orderRepository.sumRevenueBySellerIdBetweenDates(sellerId, java.time.LocalDateTime.of(1970, 1, 1, 0, 0),
                java.time.LocalDateTime.now());
    }

    @Override
    @Transactional(readOnly = true)
    public long getNewOrderCountBySellerId(Long sellerId) {
        return orderRepository.countByStoreSellerIdAndOrderStatus(sellerId, Order.OrderStatus.PLACED);
    }

    @Override
    @Transactional(readOnly = true)
    public long getProcessingOrderCountBySellerId(Long sellerId) {
        return orderRepository.countByStoreSellerIdAndOrderStatus(sellerId, Order.OrderStatus.CONFIRMED);
    }

    @Override
    @Transactional(readOnly = true)
    public long getShippedOrderCountBySellerId(Long sellerId) {
        return orderRepository.countByStoreSellerIdAndOrderStatus(sellerId, Order.OrderStatus.SHIPPED);
    }

    @Override
    @Transactional(readOnly = true)
    public long getCompletedOrderCountBySellerId(Long sellerId) {
        return orderRepository.countByStoreSellerIdAndOrderStatus(sellerId, Order.OrderStatus.DELIVERED);
    }

    @Override
    @Transactional(readOnly = true)
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
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getDailySalesData() {
        return new ArrayList<>(); // To be implemented
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getMonthlySalesData() {
        return new ArrayList<>(); // To be implemented
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, BigDecimal> getRevenueByCategory() {
        return new HashMap<>(); // To be implemented
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getSalesTrendBySellerId(Long sellerId) {
        return new ArrayList<>(); // To be implemented
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getCustomerDemographicsBySellerId(Long sellerId) {
        return new HashMap<>(); // To be implemented
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, BigDecimal> getRevenueBreakdownBySellerId(Long sellerId) {
        Map<String, BigDecimal> breakdown = new HashMap<>();
        breakdown.put("totalRevenue", getTotalRevenueBySellerId(sellerId));
        breakdown.put("monthlyRevenue", getMonthlyRevenueBySellerId(sellerId));
        breakdown.put("weeklyRevenue", getWeeklyRevenueBySellerId(sellerId));
        breakdown.put("todayRevenue", getTodayRevenueBySellerId(sellerId));
        return breakdown;
    }

    // Delivery Agent-specific dashboard method implementation
    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getRecentDeliveriesByAgentId(Long agentId, int limit) {
        return orderRepository
                .findByDeliveryAgentIdOrderByCreatedAtDesc(agentId,
                        org.springframework.data.domain.PageRequest.of(0, limit))
                .stream()
                .map(o -> Map.<String, Object>of(
                        "orderNumber", o.getOrderNumber(),
                        "status", o.getOrderStatus().toString(),
                        "totalAmount", o.getTotalAmount(),
                        "createdAt", o.getCreatedAt()))
                .toList();
    }

    // Additional Delivery Agent dashboard methods
    @Override
    @Transactional(readOnly = true)
    public long getPendingDeliveriesByAgentId(Long agentId) {
        return orderRepository.countByDeliveryAgentIdAndOrderStatus(agentId, Order.OrderStatus.PLACED);
    }

    @Override
    @Transactional(readOnly = true)
    public long getTodayDeliveriesByAgentId(Long agentId) {
        return orderRepository.countByDeliveryAgentIdAndCreatedAtAfter(agentId,
                com.eshop.app.util.DateTimeUtils.startOfDay());
    }

    @Override
    @Transactional(readOnly = true)
    public long getInTransitOrdersByAgentId(Long agentId) {
        return orderRepository.countByDeliveryAgentIdAndOrderStatus(agentId, Order.OrderStatus.SHIPPED);
    }

    @Override
    @Transactional(readOnly = true)
    public long getUrgentDeliveriesByAgentId(Long agentId) {
        return 0L; // To be implemented with urgency logic
    }

    @Override
    @Transactional(readOnly = true)
    public long getCompletedDeliveriesTodayByAgentId(Long agentId) {
        return orderRepository.countByDeliveryAgentIdAndOrderStatusAndCreatedAtAfter(agentId,
                Order.OrderStatus.DELIVERED, com.eshop.app.util.DateTimeUtils.startOfDay());
    }

    @Override
    @Transactional(readOnly = true)
    public long getCompletedDeliveriesThisWeekByAgentId(Long agentId) {
        return orderRepository.countByDeliveryAgentIdAndOrderStatusAndCreatedAtAfter(agentId,
                Order.OrderStatus.DELIVERED, com.eshop.app.util.DateTimeUtils.startOfWeek());
    }

    @Override
    @Transactional(readOnly = true)
    public long getCompletedDeliveriesThisMonthByAgentId(Long agentId) {
        return orderRepository.countByDeliveryAgentIdAndOrderStatusAndCreatedAtAfter(agentId,
                Order.OrderStatus.DELIVERED, com.eshop.app.util.DateTimeUtils.startOfMonth());
    }

    @Override
    @Transactional(readOnly = true)
    public double getAverageDeliveryTimeByAgentId(Long agentId) {
        return 0.0; // To be implemented with delivery time tracking
    }

    @Override
    @Transactional(readOnly = true)
    public double getDeliverySuccessRateByAgentId(Long agentId) {
        long total = orderRepository.countByDeliveryAgentId(agentId);
        if (total == 0)
            return 0.0;
        long successful = orderRepository.countByDeliveryAgentIdAndOrderStatus(agentId, Order.OrderStatus.DELIVERED);
        return (double) successful / total * 100;
    }

    @Override
    @Transactional(readOnly = true)
    public double getCustomerRatingByAgentId(Long agentId) {
        return 0.0; // To be implemented with rating system
    }

    // Customer-specific dashboard methods
    @Override
    @Transactional(readOnly = true)
    public long getOrderCountByCustomerId(Long customerId) {
        return orderRepository.countByCustomerId(customerId);
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<java.util.Map<String, Object>> getRecentOrdersByCustomerId(Long customerId, int limit) {
        Page<Order> orders = orderRepository.findByCustomerId(customerId,
                org.springframework.data.domain.PageRequest.of(0, limit));
        return orders.stream()
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
    @Transactional(readOnly = true)
    public java.math.BigDecimal getTotalSpentByCustomerId(Long customerId) {
        BigDecimal total = orderRepository.sumTotalAmountByCustomerId(customerId);
        return total != null ? total : BigDecimal.ZERO;
    }

    @Override
    @Transactional(readOnly = true)
    public java.math.BigDecimal getAverageOrderValueByCustomerId(Long customerId) {
        long count = orderRepository.countByCustomerId(customerId);
        if (count == 0)
            return BigDecimal.ZERO;
        BigDecimal total = orderRepository.sumTotalAmountByCustomerId(customerId);
        return total != null ? total.divide(BigDecimal.valueOf(count), 2, java.math.RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
    }
}
