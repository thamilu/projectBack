package com.eshop.app.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Listener for product-related domain events.
 * All listeners are async and execute after transaction commit.
 * 
 * @since 2.0
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ProductEventListener {
    
    /**
     * Handle product creation events.
     * Triggers indexing, notifications, etc.
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onProductCreated(ProductCreatedEvent event) {
        log.info("Product created: ID={}, SKU={}, Name={}", 
            event.getProduct().getId(),
            event.getProduct().getSku(),
            event.getProduct().getName());
        
        // Trigger search index update (integrate with Elasticsearch/Solr/OpenSearch)
        log.debug("Search index update required for product ID: {}", event.getProduct().getId());
        // Example: searchIndexService.indexProduct(event.getProduct());
        
        // Send notifications to relevant parties (sellers, admins, subscribers)
        log.debug("Notifications to be sent for new product: {}", event.getProduct().getSku());
        // Example: notificationService.notifyProductCreated(event.getProduct());
    }
    
    /**
     * Handle stock change events.
     * Records audit trail and triggers alerts if needed.
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onStockChanged(StockChangedEvent event) {
        log.info("Stock changed for product {}: {} -> {} (delta: {}, reason: {})",
            event.getProductId(),
            event.getPreviousStock(),
            event.getNewStock(),
            event.getDelta(),
            event.getReason());
        
        // Record in stock movement audit table for compliance and analytics
        log.debug("Recording stock movement audit: productId={}, delta={}", 
            event.getProductId(), event.getDelta());
        // Example: stockAuditRepository.save(new StockMovement(event));
    }
    
    /**
     * Handle low stock alerts.
     * Sends notifications to sellers/admins.
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onLowStock(LowStockEvent event) {
        log.warn("Low stock alert for product: ID={}, SKU={}, Stock={}",
            event.getProduct().getId(),
            event.getProduct().getSku(),
            event.getProduct().getStockQuantity());
        
        // Send email/SMS notifications to sellers and admins
        log.info("Sending low stock notifications for product: {}", event.getProduct().getSku());
        // Example: notificationService.sendLowStockAlert(event.getProduct());
        
        // Create low stock report entry for dashboard and analytics
        log.debug("Creating low stock report entry for product ID: {}", event.getProduct().getId());
        // Example: reportService.createLowStockEntry(event.getProduct());
    }
}
