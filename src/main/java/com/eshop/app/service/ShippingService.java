package com.eshop.app.service;

import com.eshop.app.dto.request.ShippingRequest;
import com.eshop.app.dto.request.TrackingUpdateRequest;
import com.eshop.app.dto.response.PageResponse;
import com.eshop.app.dto.response.ShippingResponse;
import com.eshop.app.entity.Shipping;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Shipping service interface for logistics management
 * Handles shipping creation, tracking, and delivery management
 * 
 * Time Complexity: All operations O(1) or O(log n)
 * Space Complexity: O(1) for processing, O(n) for batch operations
 */
public interface ShippingService {
    
    /**
     * Create shipping for an order
     * 
     * @param request Shipping details including address, method, carrier
     * @return Shipping response with tracking information
     * 
     * Business Rules:
     * - Order must be in CONFIRMED status
     * - Calculates shipping cost based on method and destination
     * - Generates tracking number
     * - Sets estimated delivery date
     */
    ShippingResponse createShipping(ShippingRequest request);
    
    /**
     * Get shipping by order ID
     */
    ShippingResponse getShippingByOrderId(Long orderId);
    
    /**
     * Get shipping by tracking number
     */
    ShippingResponse getShippingByTrackingNumber(String trackingNumber);
    
    /**
     * Get user's shipping history
     */
    PageResponse<ShippingResponse> getUserShippings(Long userId, Pageable pageable);
    
    /**
     * Get shippings by status
     */
    PageResponse<ShippingResponse> getShippingsByStatus(Shipping.ShippingStatus status, Pageable pageable);
    
    /**
     * Get shippings by carrier
     */
    PageResponse<ShippingResponse> getShippingsByCarrier(Shipping.ShippingCarrier carrier, Pageable pageable);
    
    /**
     * Update tracking information
     * 
     * @param request Tracking update with new status and location
     * @return Updated shipping response
     * 
     * Business Rules:
     * - Updates shipping status based on tracking info
     * - Notifies customer of status changes
     * - Updates estimated delivery if needed
     */
    ShippingResponse updateTracking(TrackingUpdateRequest request);
    
    /**
     * Mark order as shipped
     */
    ShippingResponse markAsShipped(Long shippingId, String trackingNumber);
    
    /**
     * Mark order as delivered
     */
    ShippingResponse markAsDelivered(Long shippingId, String deliveredTo);
    
    /**
     * Calculate shipping cost
     * 
     * @param method Shipping method (standard, expedited, etc.)
     * @param weight Package weight in kg
     * @param destination Destination address for distance calculation
     * @return Calculated shipping cost
     */
    BigDecimal calculateShippingCost(Shipping.ShippingMethod method, 
                                    BigDecimal weight, 
                                    String destination);
    
    /**
     * Get estimated delivery date
     */
    LocalDateTime getEstimatedDeliveryDate(Shipping.ShippingMethod method, String destination);
    
    /**
     * Get in-transit shippings
     */
    PageResponse<ShippingResponse> getInTransitShippings(Pageable pageable);
    
    /**
     * Get overdue deliveries (past estimated delivery date)
     */
    List<ShippingResponse> getOverdueDeliveries();
    
    /**
     * Get delivery performance statistics
     */
    Object getDeliveryStatistics(LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Update shipping address (before shipping)
     */
    ShippingResponse updateShippingAddress(Long shippingId, ShippingRequest.Address newAddress);
    
    /**
     * Cancel shipping (before shipped)
     */
    ShippingResponse cancelShipping(Long shippingId, String reason);
    
    /**
     * Get available shipping methods for destination
     */
    List<Object> getAvailableShippingMethods(String destination, BigDecimal weight);
}