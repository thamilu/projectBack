package com.eshop.app.order.application.service.impl;

import com.eshop.app.cart.domain.entity.Cart;
import com.eshop.app.cart.domain.entity.CartItem;
import com.eshop.app.cart.domain.repository.CartRepository;
import com.eshop.app.cart.shared.exception.EmptyCartException;
import com.eshop.app.catalog.domain.entity.Product;

import com.eshop.app.core.util.SecurityUtils;
import com.eshop.app.inventory.shared.exception.InsufficientStockException;
import com.eshop.app.order.api.request.CheckoutRequest;
import com.eshop.app.order.api.response.OrderResponse;
import com.eshop.app.order.application.port.in.CheckoutUseCase;
import com.eshop.app.order.domain.entity.Order;
import com.eshop.app.order.domain.entity.OrderItem;
import com.eshop.app.order.domain.repository.OrderRepository;
import com.eshop.app.order.application.mapper.OrderMapper;
import com.eshop.app.core.exception.business.ResourceNotFoundException;
import com.eshop.app.user.domain.entity.User;
import com.eshop.app.user.domain.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import java.util.HashSet;
import java.util.Set;

@Service
@Transactional
@RequiredArgsConstructor
public class CheckoutUseCaseImpl implements CheckoutUseCase {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final com.eshop.app.inventory.application.port.in.ReserveStockUseCase reserveStockUseCase;
    private final com.eshop.app.inventory.application.port.in.UpdateInventoryUseCase updateInventoryUseCase;
    private final UserRepository userRepository;
    private final OrderMapper orderMapper;
    private final OrderNumberGenerator orderNumberGenerator;

    @Override
    public OrderResponse checkoutAnonymousCart(String cartCode, CheckoutRequest request) {
        Cart cart = cartRepository.findByCartCode(cartCode)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found with code: " + cartCode));

        if (cart.getItems().isEmpty()) {
            throw new EmptyCartException("Cannot checkout empty cart");
        }

        return processCheckout(cart, request, null);
    }

    @Override
    public OrderResponse checkoutAuthenticatedCart(String cartCode, CheckoutRequest request) {
        Cart cart = cartRepository.findByCartCode(cartCode)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found with code: " + cartCode));

        Long currentUserId = SecurityUtils.getCurrentUserId().map(Long::parseLong)
                .orElseThrow(() -> new ResourceNotFoundException("User not authenticated"));

        if (cart.getUser() == null || !cart.getUser().getId().equals(currentUserId)) {
            throw new ResourceNotFoundException("Cart not found or access denied");
        }

        if (cart.getItems().isEmpty()) {
            throw new EmptyCartException("Cannot checkout empty cart");
        }

        return processCheckout(cart, request, currentUserId);
    }

    private OrderResponse processCheckout(Cart cart, CheckoutRequest request, Long userId) {
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
            subtotal = subtotal.add(product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
            
            // Confirm sale in inventory
            updateInventoryUseCase.confirmSale(product.getId(), cartItem.getQuantity());

            OrderItem orderItem = OrderItem.builder()
                    .product(product)
                    .quantity(cartItem.getQuantity())
                    .price(product.getPrice())
                    .subtotal(product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())))
                    .build();
            orderItems.add(orderItem);
        }

        BigDecimal shippingAmount = BigDecimal.ZERO;
        BigDecimal taxAmount = BigDecimal.ZERO;
        BigDecimal totalAmount = subtotal.add(shippingAmount).add(taxAmount);

        Order.OrderBuilder orderBuilder = Order.builder()
                .orderNumber(orderNumberGenerator.generateOrderNumber())
                .totalAmount(totalAmount)
                .taxAmount(taxAmount)
                .shippingAmount(shippingAmount)
                .orderStatus(Order.OrderStatus.PLACED)
                .paymentStatus(Order.PaymentStatus.PENDING)
                .shippingAddress(request.getShippingAddress())
                .billingAddress(request.getBillingAddress())
                .phone(request.getPhone())
                .notes(request.getNotes())
                .items(orderItems);

        if (userId != null) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            orderBuilder.customer(user);
        }

        Order order = orderBuilder.build();
        orderItems.forEach(item -> item.setOrder(order));

        Order savedOrder = orderRepository.save(order);
        cart.getItems().clear();
        cartRepository.save(cart);

        return orderMapper.toOrderResponse(savedOrder);
    }
}



