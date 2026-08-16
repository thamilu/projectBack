package com.eshop.app.order.application.service.impl;

import com.eshop.app.order.api.response.OrderResponse;
import com.eshop.app.order.application.port.in.UpdateOrderUseCase;
import com.eshop.app.order.domain.entity.Order;
import com.eshop.app.order.domain.repository.OrderRepository;
import com.eshop.app.order.application.mapper.OrderMapper;
import com.eshop.app.core.events.domain.OrderStatusChangedEvent;
import com.eshop.app.core.exception.business.ResourceNotFoundException;
import com.eshop.app.user.domain.entity.User;
import com.eshop.app.user.shared.domain.enums.UserRole;
import com.eshop.app.user.domain.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class UpdateOrderUseCaseImpl implements UpdateOrderUseCase {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final OrderMapper orderMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public OrderResponse updateOrderStatus(Long orderId, String status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        Order.OrderStatus targetStatus = Order.OrderStatus.valueOf(status.toUpperCase());
        // Captured before the switch below mutates the aggregate in place —
        // Order has no history/audit trail, so this is the only point this
        // value is ever observable.
        Order.OrderStatus previousStatus = order.getOrderStatus();

        switch (targetStatus) {
            case CONFIRMED -> order.confirm();
            case SHIPPED -> order.ship();
            case DELIVERED -> order.deliver();
            case CANCELLED -> order.cancel("Administrative update");
            default -> throw new IllegalArgumentException("Unsupported status transition to " + targetStatus);
        }

        order = orderRepository.save(order);

        if (order.getOrderStatus() != previousStatus) {
            eventPublisher.publishEvent(new OrderStatusChangedEvent(
                    this,
                    order.getId(),
                    order.getOrderNumber(),
                    order.getCustomer().getId(),
                    order.getCustomer().getEmail(),
                    order.getTotalAmount(),
                    previousStatus,
                    order.getOrderStatus()));
        }

        return orderMapper.toOrderResponse(order);
    }

    @Override
    public OrderResponse updatePaymentStatus(Long orderId, String status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        Order.PaymentStatus targetStatus = Order.PaymentStatus.valueOf(status.toUpperCase());
        
        if (targetStatus == Order.PaymentStatus.PAID) {
            order.markAsPaid();
        } else {
            throw new IllegalArgumentException("Manual update to " + targetStatus + " is not yet supported via this behavior.");
        }

        order = orderRepository.save(order);
        return orderMapper.toOrderResponse(order);
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

        return orderMapper.toOrderResponse(order);
    }
}



