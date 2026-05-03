package com.eshop.app.controller;

import com.eshop.app.dto.request.ShippingRequest;
import com.eshop.app.dto.request.TrackingUpdateRequest;
import com.eshop.app.dto.response.ApiResponse;
import com.eshop.app.dto.response.PageResponse;
import com.eshop.app.dto.response.ShippingResponse;
import com.eshop.app.entity.Shipping;
import com.eshop.app.service.ShippingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.eshop.app.constants.ApiConstants;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Shipping Controller - Manages shipping operations, tracking, and delivery
 * Handles shipping creation, status updates, and tracking information
 * 
 * All endpoints require authentication via JWT Bearer token
 * Role-based access control applied per endpoint
 */
@Tag(name = "Shippings", description = "Shipping and delivery management - Track orders, update status, calculate costs")
@RestController
@RequestMapping(ApiConstants.Endpoints.SHIPPING)
public class ShippingController {
    
    private final ShippingService shippingService;
    
    public ShippingController(ShippingService shippingService) {
        this.shippingService = shippingService;
    }
    
    // ==================== Create Shipping ====================
    
    @PostMapping
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    @Operation(
        summary = "Create shipping for order",
        description = "Create shipping record with tracking information (SELLER and ADMIN only)",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<ApiResponse<ShippingResponse>> createShipping(
            @Valid @RequestBody ShippingRequest request) {
        ShippingResponse response = shippingService.createShipping(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Shipping created successfully", response));
    }
    
    // ==================== Get Shipping Information ====================
    
    @GetMapping("/order/{orderId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'SELLER', 'DELIVERY_AGENT', 'ADMIN')")
    @Operation(
        summary = "Get shipping by order ID",
        description = "Retrieve shipping information for specific order",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<ApiResponse<ShippingResponse>> getShippingByOrderId(
            @Parameter(description = "Order ID") @PathVariable Long orderId) {
        ShippingResponse response = shippingService.getShippingByOrderId(orderId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @GetMapping("/tracking/{trackingNumber}")
    @Operation(
        summary = "Track shipment by tracking number",
        description = "Get shipping status and location by tracking number (Public access)"
    )
    public ResponseEntity<ApiResponse<ShippingResponse>> getShippingByTrackingNumber(
            @Parameter(description = "Tracking Number") @PathVariable String trackingNumber) {
        ShippingResponse response = shippingService.getShippingByTrackingNumber(trackingNumber);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    @Operation(
        summary = "Get user's shipping history",
        description = "Retrieve all shippings for a specific user (paginated)",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<ApiResponse<PageResponse<ShippingResponse>>> getUserShippings(
            @Parameter(description = "User ID") @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        PageResponse<ShippingResponse> response = shippingService.getUserShippings(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    // ==================== Filter Shippings ====================
    
    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('SELLER', 'DELIVERY_AGENT', 'ADMIN')")
    @Operation(
        summary = "Get shippings by status",
        description = "Filter shippings by delivery status (SELLER, DELIVERY_AGENT, ADMIN only)",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<ApiResponse<PageResponse<ShippingResponse>>> getShippingsByStatus(
            @Parameter(description = "Shipping Status", example = "IN_TRANSIT") 
            @PathVariable Shipping.ShippingStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        PageResponse<ShippingResponse> response = shippingService.getShippingsByStatus(status, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @GetMapping("/carrier/{carrier}")
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    @Operation(
        summary = "Get shippings by carrier",
        description = "Filter shippings by shipping carrier (SELLER and ADMIN only)",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<ApiResponse<PageResponse<ShippingResponse>>> getShippingsByCarrier(
            @Parameter(description = "Shipping Carrier", example = "FEDEX") 
            @PathVariable Shipping.ShippingCarrier carrier,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        PageResponse<ShippingResponse> response = shippingService.getShippingsByCarrier(carrier, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @GetMapping("/in-transit")
    @PreAuthorize("hasAnyRole('DELIVERY_AGENT', 'ADMIN')")
    @Operation(
        summary = "Get all in-transit shipments",
        description = "Retrieve all packages currently in transit (DELIVERY_AGENT and ADMIN only)",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<ApiResponse<PageResponse<ShippingResponse>>> getInTransitShippings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("estimatedDeliveryDate").ascending());
        PageResponse<ShippingResponse> response = shippingService.getInTransitShippings(pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @GetMapping("/overdue")
    @PreAuthorize("hasAnyRole('ADMIN', 'DELIVERY_AGENT')")
    @Operation(
        summary = "Get overdue deliveries",
        description = "Get list of deliveries that are past their estimated delivery date",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<ApiResponse<List<ShippingResponse>>> getOverdueDeliveries() {
        List<ShippingResponse> response = shippingService.getOverdueDeliveries();
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    // ==================== Update Shipping ====================
    
    @PutMapping("/tracking")
    @PreAuthorize("hasAnyRole('DELIVERY_AGENT', 'ADMIN')")
    @Operation(
        summary = "Update tracking information",
        description = "Update shipping status and location (DELIVERY_AGENT and ADMIN only)",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<ApiResponse<ShippingResponse>> updateTracking(
            @Valid @RequestBody TrackingUpdateRequest request) {
        ShippingResponse response = shippingService.updateTracking(request);
        return ResponseEntity.ok(ApiResponse.success("Tracking updated successfully", response));
    }
    
    @PutMapping("/{shippingId}/mark-shipped")
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    @Operation(
        summary = "Mark order as shipped",
        description = "Update shipping status to SHIPPED with tracking number (SELLER and ADMIN only)",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<ApiResponse<ShippingResponse>> markAsShipped(
            @Parameter(description = "Shipping ID") @PathVariable Long shippingId,
            @RequestParam String trackingNumber) {
        ShippingResponse response = shippingService.markAsShipped(shippingId, trackingNumber);
        return ResponseEntity.ok(ApiResponse.success("Order marked as shipped", response));
    }
    
    @PutMapping("/{shippingId}/mark-delivered")
    @PreAuthorize("hasAnyRole('DELIVERY_AGENT', 'ADMIN')")
    @Operation(
        summary = "Mark order as delivered",
        description = "Update shipping status to DELIVERED (DELIVERY_AGENT and ADMIN only)",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<ApiResponse<ShippingResponse>> markAsDelivered(
            @Parameter(description = "Shipping ID") @PathVariable Long shippingId,
            @RequestParam String deliveredTo) {
        ShippingResponse response = shippingService.markAsDelivered(shippingId, deliveredTo);
        return ResponseEntity.ok(ApiResponse.success("Order marked as delivered", response));
    }
    
    @PutMapping("/{shippingId}/address")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    @Operation(
        summary = "Update shipping address",
        description = "Update shipping address before package is shipped (CUSTOMER and ADMIN only)",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<ApiResponse<ShippingResponse>> updateShippingAddress(
            @Parameter(description = "Shipping ID") @PathVariable Long shippingId,
            @Valid @RequestBody ShippingRequest.Address newAddress) {
        ShippingResponse response = shippingService.updateShippingAddress(shippingId, newAddress);
        return ResponseEntity.ok(ApiResponse.success("Shipping address updated successfully", response));
    }
    
    @PutMapping("/{shippingId}/cancel")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'SELLER', 'ADMIN')")
    @Operation(
        summary = "Cancel shipping",
        description = "Cancel shipping before package is shipped (CUSTOMER, SELLER, ADMIN only)",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<ApiResponse<ShippingResponse>> cancelShipping(
            @Parameter(description = "Shipping ID") @PathVariable Long shippingId,
            @RequestParam String reason) {
        ShippingResponse response = shippingService.cancelShipping(shippingId, reason);
        return ResponseEntity.ok(ApiResponse.success("Shipping cancelled successfully", response));
    }
    
    // ==================== Utility Endpoints ====================
    
    @GetMapping("/calculate-cost")
    @Operation(
        summary = "Calculate shipping cost",
        description = "Calculate shipping cost based on method, weight, and destination (Public access)"
    )
    public ResponseEntity<ApiResponse<BigDecimal>> calculateShippingCost(
            @Parameter(description = "Shipping Method", example = "STANDARD") 
            @RequestParam Shipping.ShippingMethod method,
            @Parameter(description = "Package weight in kg", example = "2.5") 
            @RequestParam BigDecimal weight,
            @Parameter(description = "Destination address") 
            @RequestParam String destination) {
        BigDecimal cost = shippingService.calculateShippingCost(method, weight, destination);
        return ResponseEntity.ok(ApiResponse.success("Shipping cost calculated", cost));
    }
    
    @GetMapping("/estimated-delivery")
    @Operation(
        summary = "Get estimated delivery date",
        description = "Calculate estimated delivery date based on method and destination (Public access)"
    )
    public ResponseEntity<ApiResponse<LocalDateTime>> getEstimatedDeliveryDate(
            @Parameter(description = "Shipping Method", example = "EXPEDITED") 
            @RequestParam Shipping.ShippingMethod method,
            @Parameter(description = "Destination address") 
            @RequestParam String destination) {
        LocalDateTime estimatedDate = shippingService.getEstimatedDeliveryDate(method, destination);
        return ResponseEntity.ok(ApiResponse.success("Estimated delivery date calculated", estimatedDate));
    }
    
    @GetMapping("/available-methods")
    @Operation(
        summary = "Get available shipping methods",
        description = "Get list of available shipping methods for destination and weight (Public access)"
    )
    public ResponseEntity<ApiResponse<List<Object>>> getAvailableShippingMethods(
            @Parameter(description = "Destination address") 
            @RequestParam String destination,
            @Parameter(description = "Package weight in kg", example = "1.5") 
            @RequestParam BigDecimal weight) {
        List<Object> methods = shippingService.getAvailableShippingMethods(destination, weight);
        return ResponseEntity.ok(ApiResponse.success("Available shipping methods retrieved", methods));
    }
    
    // ==================== Analytics & Statistics ====================
    
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Get delivery statistics",
        description = "Get delivery performance statistics for date range (ADMIN only)",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<ApiResponse<Object>> getDeliveryStatistics(
            @Parameter(description = "Start date", example = "2025-01-01T00:00:00") 
            @RequestParam LocalDateTime startDate,
            @Parameter(description = "End date", example = "2025-12-31T23:59:59") 
            @RequestParam LocalDateTime endDate) {
        Object statistics = shippingService.getDeliveryStatistics(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success("Delivery statistics retrieved", statistics));
    }
}
