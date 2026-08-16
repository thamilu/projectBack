package com.eshop.app.order.application.service.impl;

import com.eshop.app.cart.domain.entity.Cart;
import com.eshop.app.cart.domain.entity.CartItem;
import com.eshop.app.cart.domain.repository.CartRepository;
import com.eshop.app.cart.shared.exception.EmptyCartException;
import com.eshop.app.catalog.domain.entity.Product;

import com.eshop.app.core.util.SecurityUtils;
import com.eshop.app.inventory.shared.exception.InsufficientStockException;
import com.eshop.app.order.api.request.OrderCreateRequest;
import com.eshop.app.order.api.response.OrderResponse;
import com.eshop.app.order.application.port.in.CreateOrderUseCase;
import com.eshop.app.order.domain.entity.Order;
import com.eshop.app.order.domain.entity.OrderItem;
import com.eshop.app.order.domain.repository.OrderRepository;
import com.eshop.app.order.application.mapper.OrderMapper;
import com.eshop.app.core.exception.business.ResourceNotFoundException;
import com.eshop.app.user.domain.entity.User;
import com.eshop.app.user.domain.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Service
@Transactional
@RequiredArgsConstructor
public class CreateOrderUseCaseImpl implements CreateOrderUseCase {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final com.eshop.app.inventory.application.port.in.ReserveStockUseCase reserveStockUseCase;
    private final com.eshop.app.inventory.application.port.in.UpdateInventoryUseCase updateInventoryUseCase;
    private final UserRepository userRepository;
    private final OrderMapper orderMapper;
    private final OrderNumberGenerator orderNumberGenerator;

    @Value("${app.business.default-tax-rate:0.10}")
    private BigDecimal defaultTaxRate;

    @Override
    public OrderResponse createOrder(OrderCreateRequest request) {
        Long userId = SecurityUtils.getCurrentUserId().map(Long::parseLong)
                .orElseThrow(() -> new ResourceNotFoundException("User not authenticated"));

        User customer = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new EmptyCartException("Cannot create order from empty cart");
        }

        // Use Inventory UseCase for stock reservation
        for (CartItem cartItem : cart.getItems()) {
            boolean reserved = reserveStockUseCase.reserveStock(cartItem.getProduct().getId(), cartItem.getQuantity());
            if (!reserved) {
                throw new InsufficientStockException("Insufficient stock for product: " + cartItem.getProduct().getName());
            }
        }

        Set<OrderItem> orderItems = new HashSet<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        for (CartItem cartItem : cart.getItems()) {
            Product product = cartItem.getProduct();
            OrderItem orderItem = OrderItem.builder()
                    .product(product)
                    .quantity(cartItem.getQuantity())
                    .price(cartItem.getPrice())
                    .discountAmount(BigDecimal.ZERO)
                    .build();

            orderItems.add(orderItem);
            subtotal = subtotal.add(orderItem.getPrice().multiply(BigDecimal.valueOf(orderItem.getQuantity())));
            
            // Confirm sale in inventory
            updateInventoryUseCase.confirmSale(product.getId(), cartItem.getQuantity());
        }

        BigDecimal shippingAmount = BigDecimal.valueOf(10.00);
        BigDecimal taxAmount = subtotal.multiply(defaultTaxRate);
        BigDecimal totalAmount = subtotal.add(shippingAmount).add(taxAmount);

        Order order = Order.builder()
                .orderNumber(orderNumberGenerator.generateOrderNumber())
                .customer(customer)
                .shippingAddress(request.getShippingAddress())
                .billingAddress(request.getBillingAddress() != null ? request.getBillingAddress() : request.getShippingAddress())
                .phone(request.getPhone())
                .notes(request.getNotes())
                .items(orderItems)
                .totalAmount(totalAmount)
                .shippingAmount(shippingAmount)
                .taxAmount(taxAmount)
                .orderStatus(Order.OrderStatus.PLACED)
                .paymentStatus(Order.PaymentStatus.PENDING)
                .build();

        orderItems.forEach(item -> item.setOrder(order));

        Order savedOrder = orderRepository.save(order);

        cart.getItems().clear();
        cart.calculateTotalAmount();
        cartRepository.save(cart);

        return orderMapper.toOrderResponse(savedOrder);
    }
}



