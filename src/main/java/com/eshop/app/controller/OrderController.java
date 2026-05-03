package com.eshop.app.controller;

import com.eshop.app.dto.request.OrderCreateRequest;
import com.eshop.app.dto.response.ApiResponse;
import com.eshop.app.dto.response.OrderResponse;
import com.eshop.app.dto.response.PageResponse;
import com.eshop.app.service.OrderService;
import com.eshop.app.util.ControllerResponseUtils;
import com.eshop.app.util.PaginationUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.eshop.app.constants.ApiConstants;

@Tag(name = "Orders", description = "Order management - create, track, and manage customer orders")
@RestController
@RequestMapping(ApiConstants.Endpoints.ORDERS)
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    @Operation(summary = "Create new order", description = "Create a new order from cart items. Available for CUSTOMER and ADMIN roles.", security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @Valid @RequestBody OrderCreateRequest request) {
        OrderResponse response = orderService.createOrder(request);
        return ControllerResponseUtils.created("Order created successfully", response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'SELLER', 'DELIVERY_AGENT', 'ADMIN')")
    @Operation(summary = "Get order by ID", description = "Retrieve order details by order ID. Accessible by all authenticated roles.", security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(
            @Parameter(description = "Order ID") @PathVariable Long id) {
        OrderResponse response = orderService.getOrderById(id);
        return ControllerResponseUtils.ok(response);
    }

    @GetMapping("/number/{orderNumber}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'SELLER', 'DELIVERY_AGENT', 'ADMIN')")
    @Operation(summary = "Get order by order number", description = "Retrieve order details by order number (e.g., ORD-20251202-001).", security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderByOrderNumber(
            @Parameter(description = "Order Number") @PathVariable String orderNumber) {
        OrderResponse response = orderService.getOrderByOrderNumber(orderNumber);
        return ControllerResponseUtils.ok(response);
    }

    @GetMapping("/my-orders")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<OrderResponse>>> getMyOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PaginationUtils.createPageableWithCreatedAtDesc(page, size);
        PageResponse<OrderResponse> response = orderService.getMyOrders(pageable);
        return ControllerResponseUtils.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<OrderResponse>>> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PaginationUtils.createPageableWithCreatedAtDesc(page, size);
        PageResponse<OrderResponse> response = orderService.getAllOrders(pageable);
        return ControllerResponseUtils.ok(response);
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<OrderResponse>>> getOrdersByStatus(
            @PathVariable String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PaginationUtils.createPageableWithCreatedAtDesc(page, size);
        PageResponse<OrderResponse> response = orderService.getOrdersByStatus(status, pageable);
        return ControllerResponseUtils.ok(response);
    }

    @GetMapping("/store/{storeId}")
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<OrderResponse>>> getOrdersByStore(
            @PathVariable Long storeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PaginationUtils.createPageableWithCreatedAtDesc(page, size);
        PageResponse<OrderResponse> response = orderService.getOrdersByStore(storeId, pageable);
        return ControllerResponseUtils.ok(response);
    }

    @PutMapping("/{orderId}/status")
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestParam String status) {
        OrderResponse response = orderService.updateOrderStatus(orderId, status);
        return ControllerResponseUtils.ok("Order status updated", response);
    }

    @PutMapping("/{orderId}/payment-status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<OrderResponse>> updatePaymentStatus(
            @PathVariable Long orderId,
            @RequestParam String status) {
        OrderResponse response = orderService.updatePaymentStatus(orderId, status);
        return ControllerResponseUtils.ok("Payment status updated", response);
    }

    @PutMapping("/{orderId}/assign-delivery-agent")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<OrderResponse>> assignDeliveryAgent(
            @PathVariable Long orderId,
            @RequestParam Long agentId) {
        OrderResponse response = orderService.assignDeliveryAgent(orderId, agentId);
        return ControllerResponseUtils.ok("Delivery agent assigned", response);
    }

    @GetMapping("/delivery/my-deliveries")
    @PreAuthorize("hasAnyRole('DELIVERY_AGENT', 'ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<OrderResponse>>> getDeliveryAgentOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PaginationUtils.createPageableWithCreatedAtDesc(page, size);
        PageResponse<OrderResponse> response = orderService.getDeliveryAgentOrders(pageable);
        return ControllerResponseUtils.ok(response);
    }

    @GetMapping("/seller")
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    @Operation(summary = "Get seller orders", description = "Retrieve all orders containing items from the authenticated seller's stores.", security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<PageResponse<OrderResponse>>> getSellerOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PaginationUtils.createPageableWithCreatedAtDesc(page, size);
        PageResponse<OrderResponse> response = orderService.getSellerOrders(pageable);
        return ControllerResponseUtils.ok(response);
    }
}
