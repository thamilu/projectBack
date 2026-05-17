package com.eshop.app.order.application.port.in;

import com.eshop.app.order.api.response.OrderResponse;
import com.eshop.app.core.api.response.PageResponse;
import org.springframework.data.domain.Pageable;

/**
 * Use case for retrieving order information.
 */
public interface GetOrderUseCase {
    OrderResponse getOrderById(Long id);
    OrderResponse getOrderByOrderNumber(String orderNumber);
    PageResponse<OrderResponse> getMyOrders(Pageable pageable);
    PageResponse<OrderResponse> getAllOrders(Pageable pageable);
    PageResponse<OrderResponse> getOrdersByStatus(String status, Pageable pageable);
    PageResponse<OrderResponse> getOrdersByStore(Long storeId, Pageable pageable);
    PageResponse<OrderResponse> getDeliveryAgentOrders(Pageable pageable);
    PageResponse<OrderResponse> getSellerOrders(Pageable pageable);
}


