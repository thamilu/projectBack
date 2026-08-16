package com.eshop.app.notification.application.service;

import com.eshop.app.catalog.domain.entity.Product;
import java.math.BigDecimal;

public interface NotificationService {
    void notifyProductCreated(Product product);
    void sendLowStockAlert(Product product);

    /**
     * Sends an order confirmation email. Takes detached fields rather than
     * an {@code Order} entity deliberately: this is always invoked from an
     * {@code @Async} event listener with no active Hibernate session, and
     * {@code Order.customer} is a LAZY association — passing the entity
     * would throw {@code LazyInitializationException} the moment {@code
     * order.getCustomer().getEmail()} is accessed.
     */
    void sendOrderConfirmation(
            String orderNumber, String customerEmail, BigDecimal totalAmount, String orderStatus);
}
