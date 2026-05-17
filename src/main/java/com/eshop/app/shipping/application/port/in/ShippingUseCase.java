package com.eshop.app.shipping.application.port.in;

import com.eshop.app.shipping.api.request.ShippingRequest;
import com.eshop.app.shipping.api.request.TrackingUpdateRequest;
import com.eshop.app.core.api.response.PageResponse;
import com.eshop.app.shipping.api.response.ShippingResponse;
import com.eshop.app.shipping.domain.model.ShippingCarrier;
import com.eshop.app.shipping.domain.model.ShippingMethod;
import com.eshop.app.shipping.domain.model.ShippingStatus;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Inbound Port for Shipping Use Cases.
 */
public interface ShippingUseCase {

    ShippingResponse createShipping(ShippingRequest request);

    ShippingResponse getShippingByOrderId(Long orderId);

    ShippingResponse getShippingByTrackingNumber(String trackingNumber);

    PageResponse<ShippingResponse> getUserShippings(Long userId, Pageable pageable);

    PageResponse<ShippingResponse> getShippingsByStatus(ShippingStatus status, Pageable pageable);

    PageResponse<ShippingResponse> getShippingsByCarrier(ShippingCarrier carrier, Pageable pageable);

    ShippingResponse updateTracking(TrackingUpdateRequest request);

    ShippingResponse markAsShipped(Long shippingId, String trackingNumber);

    ShippingResponse markAsDelivered(Long shippingId, String deliveredTo);

    BigDecimal calculateShippingCost(ShippingMethod method,
            BigDecimal weight,
            String destination);

    LocalDateTime getEstimatedDeliveryDate(ShippingMethod method, String destination);

    PageResponse<ShippingResponse> getInTransitShippings(Pageable pageable);

    List<ShippingResponse> getOverdueDeliveries();

    Object getDeliveryStatistics(LocalDateTime startDate, LocalDateTime endDate);

    ShippingResponse updateShippingAddress(Long shippingId, ShippingRequest.Address newAddress);

    ShippingResponse cancelShipping(Long shippingId, String reason);

    List<Object> getAvailableShippingMethods(String destination, BigDecimal weight);
}

