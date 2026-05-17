package com.eshop.app.notification.application.service;

import com.eshop.app.catalog.domain.entity.Product;





import com.eshop.app.order.domain.entity.Order;

public interface NotificationService {
    void notifyProductCreated(Product product);
    void sendLowStockAlert(Product product);
    void sendOrderConfirmation(Order order);
}
