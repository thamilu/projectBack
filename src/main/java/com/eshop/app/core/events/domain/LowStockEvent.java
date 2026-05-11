package com.eshop.app.core.events.domain;

import com.eshop.app.entity.Product;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Domain event published when product stock falls below the low-stock threshold.
 *
 * <p>Triggers seller and admin notifications, and creates low-stock report entries.
 *
 * @see com.eshop.app.core.events.listeners.ProductEventListener
 * @since 2.0
 */
@Getter
public class LowStockEvent extends ApplicationEvent {

    private final Product product;

    public LowStockEvent(Object source, Product product) {
        super(source);
        this.product = product;
    }
}
