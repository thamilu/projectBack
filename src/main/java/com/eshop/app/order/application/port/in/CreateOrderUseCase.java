package com.eshop.app.order.application.port.in;

import com.eshop.app.order.api.request.OrderCreateRequest;
import com.eshop.app.order.api.response.OrderResponse;

/**
 * Use case for creating a new order.
 */
public interface CreateOrderUseCase {
    OrderResponse createOrder(OrderCreateRequest request);
}
