package com.eshop.app.core.events.domain;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;

/**
 * Domain event published when a product's stock quantity changes.
 *
 * <p>Carries a full audit trail of the stock delta (who, what, when) to support
 * compliance tracking and stock movement analytics.
 *
 * @see com.eshop.app.core.events.listeners.ProductEventListener
 * @since 2.0
 */
@Getter
public class StockChangedEvent extends ApplicationEvent {

    private final Long productId;
    private final Integer previousStock;
    private final Integer newStock;
    private final Integer delta;
    private final String reason;
    private final LocalDateTime eventTimestamp;

    public StockChangedEvent(Object source, Long productId, int previousStock, int newStock, int delta, String reason) {
        super(source);
        this.productId = productId;
        this.previousStock = previousStock;
        this.newStock = newStock;
        this.delta = delta;
        this.reason = reason;
        this.eventTimestamp = LocalDateTime.now();
    }
}
