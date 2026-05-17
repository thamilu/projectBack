package com.eshop.app.order.application.service.impl;

import com.eshop.app.core.util.SecurityUtils;
import com.eshop.app.order.api.response.OrderResponse;
import com.eshop.app.order.application.port.in.GetOrderUseCase;
import com.eshop.app.order.domain.entity.Order;
import com.eshop.app.order.domain.repository.OrderRepository;
import com.eshop.app.core.api.response.PageResponse;
import com.eshop.app.order.application.mapper.OrderMapper;
import com.eshop.app.core.exception.business.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class GetOrderUseCaseImpl implements GetOrderUseCase {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;

    @Override
    public OrderResponse getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));
        return orderMapper.toOrderResponse(order);
    }

    @Override
    public OrderResponse getOrderByOrderNumber(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with number: " + orderNumber));
        return orderMapper.toOrderResponse(order);
    }

    @Override
    public PageResponse<OrderResponse> getMyOrders(Pageable pageable) {
        Long userId = SecurityUtils.getCurrentUserId().map(Long::parseLong)
                .orElseThrow(() -> new ResourceNotFoundException("User not authenticated"));
        Page<Order> orderPage = orderRepository.findByCustomerId(userId, pageable);
        return PageResponse.of(orderPage, orderMapper::toOrderResponse);
    }

    @Override
    public PageResponse<OrderResponse> getAllOrders(Pageable pageable) {
        Page<Order> orderPage = orderRepository.findAll(pageable);
        return PageResponse.of(orderPage, orderMapper::toOrderResponse);
    }

    @Override
    public PageResponse<OrderResponse> getOrdersByStatus(String status, Pageable pageable) {
        Order.OrderStatus orderStatus = Order.OrderStatus.valueOf(status.toUpperCase());
        Page<Order> orderPage = orderRepository.findByOrderStatus(orderStatus, pageable);
        return PageResponse.of(orderPage, orderMapper::toOrderResponse);
    }

    @Override
    public PageResponse<OrderResponse> getOrdersByStore(Long storeId, Pageable pageable) {
        Page<Order> orderPage = orderRepository.findByStoreId(storeId, pageable);
        return PageResponse.of(orderPage, orderMapper::toOrderResponse);
    }

    @Override
    public PageResponse<OrderResponse> getDeliveryAgentOrders(Pageable pageable) {
        Long agentId = SecurityUtils.getCurrentUserId().map(Long::parseLong)
                .orElseThrow(() -> new ResourceNotFoundException("User not authenticated"));
        Page<Order> orderPage = orderRepository.findByDeliveryAgentId(agentId, pageable);
        return PageResponse.of(orderPage, orderMapper::toOrderResponse);
    }

    @Override
    public PageResponse<OrderResponse> getSellerOrders(Pageable pageable) {
        Long sellerId = SecurityUtils.getCurrentUserId().map(Long::parseLong)
                .orElseThrow(() -> new ResourceNotFoundException("User not authenticated"));
        Page<Order> orderPage = orderRepository.findByStoreSellerId(sellerId, pageable);
        return PageResponse.of(orderPage, orderMapper::toOrderResponse);
    }
}





