package com.eshop.app.order.application.port.in;

import com.eshop.app.order.api.response.OrderResponse;

/**
 * Use case for updating order status and assignments.
 */
public interface UpdateOrderUseCase {
    OrderResponse updateOrderStatus(Long orderId, String status);
    OrderResponse updatePaymentStatus(Long orderId, String status);
    OrderResponse assignDeliveryAgent(Long orderId, Long agentId);
}
