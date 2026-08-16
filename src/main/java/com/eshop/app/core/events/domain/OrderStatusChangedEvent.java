package com.eshop.app.core.events.domain;

import com.eshop.app.order.domain.entity.Order;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Domain event published when an order's status changes.
 *
 * <p>Carries a detached snapshot of the fields listeners need — not the
 * {@link Order} entity itself. Listeners run {@code @Async} after the
 * originating transaction commits, with no active Hibernate session; holding
 * a reference to the entity and later traversing a LAZY association (e.g.
 * {@code order.getCustomer().getEmail()}) would throw {@code
 * LazyInitializationException}. This is the same class of bug that left
 * {@code NotificationService.sendOrderConfirmation(Order)} unusable from an
 * async context — avoided here by design.
 *
 * @see com.eshop.app.core.events.listener.OrderEventListener
 */
@Getter
public class OrderStatusChangedEvent extends ApplicationEvent {

    private final Long orderId;
    private final String orderNumber;
    private final Long customerId;
    private final String customerEmail;
    private final BigDecimal totalAmount;
    private final Order.OrderStatus previousStatus;
    private final Order.OrderStatus newStatus;
    private final LocalDateTime eventTimestamp;

    public OrderStatusChangedEvent(
            Object source,
            Long orderId,
            String orderNumber,
            Long customerId,
            String customerEmail,
            BigDecimal totalAmount,
            Order.OrderStatus previousStatus,
            Order.OrderStatus newStatus) {
        super(source);
        this.orderId = orderId;
        this.orderNumber = orderNumber;
        this.customerId = customerId;
        this.customerEmail = customerEmail;
        this.totalAmount = totalAmount;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.eventTimestamp = LocalDateTime.now();
    }
}
