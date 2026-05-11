package com.eshop.app.service;

import com.eshop.app.entity.Product;

public interface NotificationService {
    void notifyProductCreated(Product product);
    void sendLowStockAlert(Product product);
}
