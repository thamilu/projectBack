package com.eshop.app.order.application.service.impl;

import com.eshop.app.cart.domain.entity.Cart;
import com.eshop.app.cart.domain.entity.CartItem;
import com.eshop.app.cart.shared.exception.EmptyCartException;
import com.eshop.app.catalog.domain.entity.Product;
import com.eshop.app.inventory.application.port.in.ReserveStockUseCase;
import com.eshop.app.inventory.application.port.in.UpdateInventoryUseCase;
import com.eshop.app.inventory.shared.exception.InsufficientStockException;
import com.eshop.app.order.domain.entity.Order;
import com.eshop.app.order.domain.entity.OrderItem;
import com.eshop.app.user.domain.entity.User;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Shared factory component for constructing Order aggregates. Handles the multi-step transactional
 * logic of stock reservation, inventory confirmation, item mapping, tax/shipping computation, and
 * circular reference mapping.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderFactory {

    private final ReserveStockUseCase reserveStockUseCase;
    private final UpdateInventoryUseCase updateInventoryUseCase;
    private final OrderNumberGenerator orderNumberGenerator;

    /**
     * Constructs and persists a transient Order entity from a customer Cart. Enforces domain
     * boundaries by performing stock reservations and sales confirmations.
     *
     * @param cart the shopping cart
     * @param customer the authenticated user ordering, optional
     * @param shippingAddress customer's shipping address
     * @param billingAddress customer's billing address, defaults to shipping if null
     * @param phone customer contact phone
     * @param notes order notes
     * @param shippingAmount shipping charges
     * @param taxRate tax rate percentage
     * @return the fully populated Order entity
     */
    public Order createOrder(
            Cart cart,
            User customer,
            String shippingAddress,
            String billingAddress,
            String phone,
            String notes,
            BigDecimal shippingAmount,
            BigDecimal taxRate) {
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            log.warn("Attempted to construct an order from an empty cart");
            throw new EmptyCartException("Cannot create order from empty cart");
        }

        // 1. Reserve stock for all items in the cart
        log.info("Starting stock reservation for cart items");
        for (CartItem cartItem : cart.getItems()) {
            boolean reserved =
                    reserveStockUseCase.reserveStock(
                            cartItem.getProduct().getId(), cartItem.getQuantity());
            if (!reserved) {
                log.error(
                        "Insufficient stock for product id={}, name='{}'",
                        cartItem.getProduct().getId(),
                        cartItem.getProduct().getName());
                throw new InsufficientStockException(
                        "Insufficient stock for product: " + cartItem.getProduct().getName());
            }
        }

        // 2. Build order items & confirm inventory sales
        Set<OrderItem> orderItems = new HashSet<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        log.info("Processing order items mapping and inventory updates");
        for (CartItem cartItem : cart.getItems()) {
            Product product = cartItem.getProduct();

            // Confirm sale in inventory
            updateInventoryUseCase.confirmSale(product.getId(), cartItem.getQuantity());

            OrderItem orderItem =
                    OrderItem.builder()
                            .product(product)
                            .quantity(cartItem.getQuantity())
                            .price(product.getPrice()) // Standardise pricing from product
                            .discountAmount(BigDecimal.ZERO)
                            .subtotal(
                                    product.getPrice()
                                            .multiply(BigDecimal.valueOf(cartItem.getQuantity())))
                            .build();

            orderItems.add(orderItem);
            subtotal = subtotal.add(orderItem.getSubtotal());
        }

        // 3. Compute totals
        BigDecimal taxAmount = subtotal.multiply(taxRate);
        BigDecimal totalAmount = subtotal.add(shippingAmount).add(taxAmount);

        // 4. Construct Order aggregate
        Order order =
                Order.builder()
                        .orderNumber(orderNumberGenerator.generateOrderNumber())
                        .customer(customer)
                        .shippingAddress(shippingAddress)
                        .billingAddress(billingAddress != null ? billingAddress : shippingAddress)
                        .phone(phone)
                        .notes(notes)
                        .items(orderItems)
                        .totalAmount(totalAmount)
                        .shippingAmount(shippingAmount)
                        .taxAmount(taxAmount)
                        .orderStatus(Order.OrderStatus.PLACED)
                        .paymentStatus(Order.PaymentStatus.PENDING)
                        .build();

        // Establish circular back-reference from children to parent
        orderItems.forEach(item -> item.setOrder(order));

        log.info("OrderFactory successfully constructed Order Number: {}", order.getOrderNumber());
        return order;
    }
}
