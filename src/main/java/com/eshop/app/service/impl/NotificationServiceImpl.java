package com.eshop.app.service.impl;

import com.eshop.app.entity.Product;
import com.eshop.app.service.EmailService;
import com.eshop.app.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final EmailService emailService;

    @Value("${app.admin.email:admin@eshop.com}")
    private String adminEmail;

    @Override
    public void notifyProductCreated(Product product) {
        log.info("Sending product creation notification for product: {}", product.getSku());
        String subject = "New Product Created: " + product.getName();
        String body = String.format("A new product has been created.\nSKU: %s\nName: %s\nPrice: %s", 
                product.getSku(), product.getName(), product.getPrice());
        
        emailService.sendEmail(adminEmail, subject, body);
    }

    @Override
    public void sendLowStockAlert(Product product) {
        log.warn("Sending low stock alert for product: {}", product.getSku());
        String subject = "Low Stock Alert: " + product.getSku();
        String body = String.format("Product stock is low.\nSKU: %s\nName: %s\nCurrent Stock: %d\nReorder Level: %d", 
                product.getSku(), product.getName(), product.getStockQuantity(), product.getReorderLevel());
        
        emailService.sendEmail(adminEmail, subject, body);
    }
}
