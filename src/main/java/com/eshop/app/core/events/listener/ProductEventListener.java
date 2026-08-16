package com.eshop.app.core.events.listener;

import com.eshop.app.catalog.application.service.SearchIndexService;
import com.eshop.app.notification.application.service.NotificationService;
import com.eshop.app.analytics.application.service.ReportService;
import com.eshop.app.inventory.domain.repository.StockMovementRepository;
import com.eshop.app.catalog.domain.repository.ProductRepository;
import com.eshop.app.inventory.domain.entity.StockMovement;
import com.eshop.app.realtime.infrastructure.socket.WsPushService;

import com.eshop.app.core.events.domain.LowStockEvent;
import com.eshop.app.core.events.domain.ProductCreatedEvent;
import com.eshop.app.core.events.domain.StockChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Listener for product-related domain events.
 *
 * <p>
 * All handlers are {@code @Async} and execute after the originating transaction
 * commits, ensuring no side-effects can roll back the primary business
 * operation.
 *
 * <p>
 * This class is intentionally side-effect only — it must not contain business
 * logic or modify entities. Its responsibility is to trigger downstream
 * reactions
 * (notifications, index updates, audit records) in a decoupled manner.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ProductEventListener {

    private final SearchIndexService searchIndexService;
    private final NotificationService notificationService;
    private final StockMovementRepository stockMovementRepository;
    private final ReportService reportService;
    private final ProductRepository productRepository;
    private final WsPushService wsPushService;

    /**
     * On product creation: trigger search indexing and notifications.
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onProductCreated(ProductCreatedEvent event) {
        log.info("Product created: ID={}, SKU={}, Name={}",
                event.getProduct().getId(),
                event.getProduct().getSku(),
                event.getProduct().getName());

        searchIndexService.indexProduct(event.getProduct());
        notificationService.notifyProductCreated(event.getProduct());
    }

    /**
     * On stock change: record audit trail for compliance and analytics.
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onStockChanged(StockChangedEvent event) {
        log.info("Stock changed for product {}: {} -> {} (delta={}, reason={})",
                event.getProductId(),
                event.getPreviousStock(),
                event.getNewStock(),
                event.getDelta(),
                event.getReason());

        productRepository.findById(event.getProductId()).ifPresent(product -> {
            stockMovementRepository.save(new StockMovement(event, product));
        });

        wsPushService.push(
                "product:" + event.getProductId(),
                "stock_changed",
                new StockChangedPushPayload(
                        event.getProductId(),
                        event.getPreviousStock(),
                        event.getNewStock(),
                        event.getDelta(),
                        event.getEventTimestamp().toString()));
    }

    /** Shape pushed to WebSocket subscribers — intentionally distinct from any REST DTO. */
    public record StockChangedPushPayload(
            Long productId, Integer previousStock, Integer newStock, Integer delta, String occurredAt) {}

    /**
     * On low stock: send seller and admin alerts.
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onLowStock(LowStockEvent event) {
        log.warn("Low stock alert: ID={}, SKU={}, Stock={}",
                event.getProduct().getId(),
                event.getProduct().getSku(),
                event.getProduct().getStockQuantity());

        notificationService.sendLowStockAlert(event.getProduct());
        reportService.createLowStockEntry(event.getProduct());
    }
}
