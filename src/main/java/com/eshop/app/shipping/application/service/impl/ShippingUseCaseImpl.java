package com.eshop.app.shipping.application.service.impl;

import com.eshop.app.core.exception.security.UnauthorizedException;
import com.eshop.app.core.util.SecurityUtils;
import com.eshop.app.order.domain.entity.Order;
import com.eshop.app.order.domain.repository.OrderItemRepository;
import com.eshop.app.order.domain.repository.OrderRepository;
import com.eshop.app.shipping.api.request.ShippingRequest;
import com.eshop.app.shipping.api.request.TrackingUpdateRequest;
import com.eshop.app.core.api.response.PageResponse;
import com.eshop.app.shipping.api.response.ShippingResponse;
import com.eshop.app.shipping.application.port.in.ShippingUseCase;
import com.eshop.app.shipping.domain.entity.Shipping;
import com.eshop.app.shipping.domain.model.ShippingCarrier;
import com.eshop.app.shipping.domain.model.ShippingMethod;
import com.eshop.app.shipping.domain.model.ShippingStatus;
import com.eshop.app.shipping.domain.repository.ShippingRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ShippingUseCaseImpl implements ShippingUseCase {

    private final ShippingRepository shippingRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    public ShippingUseCaseImpl(ShippingRepository shippingRepository, OrderRepository orderRepository,
            OrderItemRepository orderItemRepository) {
        this.shippingRepository = shippingRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
    }

    /**
     * Verifies the current principal owns the shipment (is the order's customer, a seller with an
     * item in the order, or an admin) before allowing a mutating operation. {@code @PreAuthorize}
     * on the controller only checks role, not resource ownership — without this, any CUSTOMER
     * could address-hijack or cancel any other customer's shipment by guessing {@code shippingId}.
     */
    private void verifyShippingAccess(Shipping shipping) {
        if (SecurityUtils.hasRole("ADMIN")) {
            return;
        }
        Long currentUserId = SecurityUtils.getAuthenticatedUserId();
        Order order = shipping.getOrder();
        if (currentUserId != null && order != null && order.getCustomer() != null
                && order.getCustomer().getId().equals(currentUserId)) {
            return;
        }
        if (currentUserId != null && order != null
                && orderItemRepository.existsByOrderIdAndSellerId(order.getId(), currentUserId)) {
            return;
        }
        throw new UnauthorizedException("You do not have permission to access this shipment");
    }

    @Override
    public ShippingResponse createShipping(ShippingRequest request) {
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + request.getOrderId()));

        Shipping shipping = Shipping.builder()
                .order(order)
                .trackingNumber(generateTrackingNumber())
                .carrier(request.getCarrier())
                .method(request.getMethod())
                .status(ShippingStatus.PENDING)
                .cost(request.getCost())
                .weightKg(request.getWeightKg())
                .dimensions(request.getDimensions())
                .shippingAddress(mapAddress(request.getShippingAddress()))
                .estimatedDeliveryDate(estimateDeliveryDate(request.getMethod()))
                .build();

        shipping = shippingRepository.save(shipping);
        return toResponse(shipping);
    }

    @Override
    public ShippingResponse getShippingByOrderId(Long orderId) {
        Optional<Shipping> shipping = shippingRepository.findByOrderId(orderId);
        return shipping.map(this::toResponse)
                .orElseThrow(() -> new IllegalArgumentException("Shipping not found for order: " + orderId));
    }

    @Override
    public ShippingResponse getShippingByTrackingNumber(String trackingNumber) {
        Optional<Shipping> shipping = shippingRepository.findByTrackingNumber(trackingNumber);
        return shipping.map(this::toResponse)
                .orElseThrow(() -> new IllegalArgumentException("Shipping not found for tracking: " + trackingNumber));
    }

    @Override
    public PageResponse<ShippingResponse> getUserShippings(Long userId, Pageable pageable) {
        Page<Shipping> page = shippingRepository.findByUserId(userId, pageable);
        return PageResponse.of(page.map(this::toResponse));
    }

    @Override
    public PageResponse<ShippingResponse> getShippingsByStatus(ShippingStatus status, Pageable pageable) {
        Page<Shipping> page = shippingRepository.findByStatus(status, pageable);
        return PageResponse.of(page.map(this::toResponse));
    }

    @Override
    public PageResponse<ShippingResponse> getShippingsByCarrier(ShippingCarrier carrier, Pageable pageable) {
        Page<Shipping> page = shippingRepository.findByCarrier(carrier, pageable);
        return PageResponse.of(page.map(this::toResponse));
    }

    @Override
    public ShippingResponse cancelShipping(Long shippingId, String reason) {
        Shipping shipping = shippingRepository.findById(shippingId)
                .orElseThrow(() -> new IllegalArgumentException("Shipping not found: " + shippingId));
        verifyShippingAccess(shipping);
        if (shipping.isInTransit()) {
            throw new IllegalStateException("Cannot cancel shipping once in transit");
        }
        shipping.setStatus(ShippingStatus.RETURNED);
        shipping.setDeliveryInstructions(reason);
        shippingRepository.save(shipping);
        return toResponse(shipping);
    }

    @Override
    public ShippingResponse updateTracking(TrackingUpdateRequest request) {
        Shipping shipping = shippingRepository.findByTrackingNumber(request.getTrackingNumber())
                .orElseThrow(() -> new IllegalArgumentException("Shipping not found for tracking: " + request.getTrackingNumber()));
        if (request.getStatus() != null) {
            shipping.setStatus(request.getStatus());
            if (request.getStatus() == ShippingStatus.DELIVERED) {
                shipping.setActualDeliveryDate(LocalDateTime.now());
            }
        }
        shippingRepository.save(shipping);
        return toResponse(shipping);
    }

    @Override
    public ShippingResponse markAsShipped(Long shippingId, String trackingNumber) {
        Shipping shipping = shippingRepository.findById(shippingId)
                .orElseThrow(() -> new IllegalArgumentException("Shipping not found: " + shippingId));
        shipping.setStatus(ShippingStatus.SHIPPED);
        shipping.setTrackingNumber(trackingNumber != null ? trackingNumber : generateTrackingNumber());
        shipping.setShippedAt(LocalDateTime.now());
        shippingRepository.save(shipping);
        return toResponse(shipping);
    }

    @Override
    public ShippingResponse markAsDelivered(Long shippingId, String deliveredTo) {
        Shipping shipping = shippingRepository.findById(shippingId)
                .orElseThrow(() -> new IllegalArgumentException("Shipping not found: " + shippingId));
        shipping.setStatus(ShippingStatus.DELIVERED);
        shipping.setDeliveredTo(deliveredTo);
        shipping.setActualDeliveryDate(LocalDateTime.now());
        shippingRepository.save(shipping);
        return toResponse(shipping);
    }

    @Override
    public java.math.BigDecimal calculateShippingCost(ShippingMethod method, java.math.BigDecimal weight, String destination) {
        java.math.BigDecimal base = switch (method) {
            case STANDARD -> java.math.BigDecimal.valueOf(5);
            case EXPEDITED -> java.math.BigDecimal.valueOf(10);
            case TWO_DAY -> java.math.BigDecimal.valueOf(12);
            case OVERNIGHT -> java.math.BigDecimal.valueOf(20);
            case SAME_DAY -> java.math.BigDecimal.valueOf(25);
            case PICKUP -> java.math.BigDecimal.ZERO;
        };
        java.math.BigDecimal perKg = java.math.BigDecimal.valueOf(1.5);
        java.math.BigDecimal w = weight != null ? weight : java.math.BigDecimal.ZERO;
        return base.add(perKg.multiply(w)).setScale(2, java.math.RoundingMode.HALF_UP);
    }

    @Override
    public LocalDateTime getEstimatedDeliveryDate(ShippingMethod method, String destination) {
        return estimateDeliveryDate(method);
    }

    @Override
    public PageResponse<ShippingResponse> getInTransitShippings(Pageable pageable) {
        Page<Shipping> page = shippingRepository.findInTransitShippings(pageable);
        return mapPage(page);
    }

    @Override
    public java.util.List<ShippingResponse> getOverdueDeliveries() {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.util.List<Shipping> list = shippingRepository.findOverdueDeliveries(now);
        return list.stream().map(this::toResponse).toList();
    }

    @Override
    public Object getDeliveryStatistics(LocalDateTime startDate, LocalDateTime endDate) {
        java.util.List<Object[]> byStatus = shippingRepository.countByStatus();
        java.util.List<Object[]> byCarrier = shippingRepository.countByCarrier();
        return java.util.Map.of("byStatus", byStatus, "byCarrier", byCarrier);
    }

    @Override
    public ShippingResponse updateShippingAddress(Long shippingId, com.eshop.app.shipping.api.request.ShippingRequest.Address newAddress) {
        Shipping shipping = shippingRepository.findById(shippingId)
                .orElseThrow(() -> new IllegalArgumentException("Shipping not found: " + shippingId));
        verifyShippingAccess(shipping);
        if (shipping.getStatus() != null && shipping.getStatus() != ShippingStatus.PENDING) {
            throw new IllegalStateException("Shipping address can only be updated before the package ships");
        }
        shipping.setShippingAddress(mapAddress(newAddress));
        shippingRepository.save(shipping);
        return toResponse(shipping);
    }

    @Override
    public java.util.List<Object> getAvailableShippingMethods(String destination, java.math.BigDecimal weight) {
        return java.util.List.of(ShippingMethod.STANDARD,
                ShippingMethod.EXPEDITED,
                ShippingMethod.TWO_DAY,
                ShippingMethod.OVERNIGHT,
                ShippingMethod.SAME_DAY,
                ShippingMethod.PICKUP);
    }

    private String generateTrackingNumber() {
        return "TRK-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }

    private LocalDateTime estimateDeliveryDate(ShippingMethod method) {
        int days;
        switch (method) {
            case STANDARD -> days = 7;
            case EXPEDITED -> days = 3;
            case OVERNIGHT -> days = 1;
            case TWO_DAY -> days = 2;
            case SAME_DAY -> days = 0;
            case PICKUP -> days = 0;
            default -> days = 5;
        }
        return LocalDateTime.now().plusDays(days);
    }

    private Shipping.Address mapAddress(ShippingRequest.Address address) {
        if (address == null) return null;
        return Shipping.Address.builder()
                .fullName(address.getFullName())
                .addressLine1(address.getAddressLine1())
                .addressLine2(address.getAddressLine2())
                .city(address.getCity())
                .state(address.getState())
                .postalCode(address.getPostalCode())
                .country(address.getCountry())
                .phoneNumber(address.getPhoneNumber())
                .build();
    }

    private ShippingResponse toResponse(Shipping s) {
        return ShippingResponse.builder()
                .id(s.getId())
                .orderId(s.getOrder() != null ? s.getOrder().getId() : null)
                .trackingNumber(s.getTrackingNumber())
                .carrier(s.getCarrier())
                .method(s.getMethod())
                .status(s.getStatus())
                .cost(s.getCost())
                .weightKg(s.getWeightKg())
                .dimensions(s.getDimensions())
                .shippingAddress(mapResponseAddress(s.getShippingAddress()))
                .shippedAt(s.getShippedAt())
                .estimatedDeliveryDate(s.getEstimatedDeliveryDate())
                .actualDeliveryDate(s.getActualDeliveryDate())
                .deliveryInstructions(s.getDeliveryInstructions())
                .signatureRequired(s.getSignatureRequired())
                .deliveredTo(s.getDeliveredTo())
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .build();
    }

    private ShippingResponse.Address mapResponseAddress(Shipping.Address address) {
        if (address == null) return null;
        return ShippingResponse.Address.builder()
                .fullName(address.getFullName())
                .addressLine1(address.getAddressLine1())
                .addressLine2(address.getAddressLine2())
                .city(address.getCity())
                .state(address.getState())
                .postalCode(address.getPostalCode())
                .country(address.getCountry())
                .phoneNumber(address.getPhoneNumber())
                .build();
    }

    private PageResponse<ShippingResponse> mapPage(Page<Shipping> page) {
        List<ShippingResponse> data = page.getContent().stream().map(this::toResponse).toList();
        PageResponse.PageMetadata metadata = PageResponse.PageMetadata.builder()
            .page(page.getNumber())
            .size(page.getSize())
            .totalElements(page.getTotalElements())
            .totalPages(page.getTotalPages())
            .hasNext(page.hasNext())
            .hasPrevious(page.hasPrevious())
            .build();
        return PageResponse.<ShippingResponse>builder()
            .data(data)
            .pagination(metadata)
            .build();
    }
}

