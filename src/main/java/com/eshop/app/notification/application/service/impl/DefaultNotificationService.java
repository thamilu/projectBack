package com.eshop.app.notification.application.service.impl;

import com.eshop.app.catalog.domain.entity.Product;
import com.eshop.app.notification.application.service.NotificationService;
import com.eshop.app.notification.application.service.EmailService;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class DefaultNotificationService implements NotificationService {

    private final EmailService emailService;

    @Value("${app.admin.email:admin@eshop.com}")
    private String adminEmail;

    @Override
    @Async("notificationExecutor")
    public void notifyProductCreated(Product product) {
        log.info("Sending product creation notification for product: {}", product.getSku());
        String subject = "New Product Created: " + product.getName();
        String body = String.format("A new product has been created.\nSKU: %s\nName: %s\nPrice: %s", 
                product.getSku(), product.getName(), product.getPrice());
        
        emailService.sendEmail(adminEmail, subject, body);
    }

    @Override
    @Async("notificationExecutor")
    public void sendLowStockAlert(Product product) {
        log.warn("Sending low stock alert for product: {}", product.getSku());
        String subject = "Low Stock Alert: " + product.getSku();
        String body = String.format("Product stock is low.\nSKU: %s\nName: %s\nCurrent Stock: %d\nReorder Level: %d", 
                product.getSku(), product.getName(), product.getStockQuantity(), product.getReorderLevel());
        emailService.sendEmail(adminEmail, subject, body);
    }

    @Override
    @Async("notificationExecutor")
    public void sendOrderConfirmation(
            String orderNumber, String customerEmail, BigDecimal totalAmount, String orderStatus) {
        log.info("Sending order confirmation for order: {}", orderNumber);
        String subject = "Order Confirmation: " + orderNumber;
        String body = String.format("Thank you for your order!\nOrder Number: %s\nTotal Amount: %s\nStatus: %s",
                orderNumber, totalAmount, orderStatus);

        emailService.sendEmail(customerEmail, subject, body);
    }
}

