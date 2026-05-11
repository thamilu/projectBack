package com.eshop.app.core.events.domain;

import com.eshop.app.entity.Product;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Domain event published when a new product is created.
 *
 * <p>Listeners execute asynchronously after the creating transaction commits,
 * ensuring decoupled side-effects (search indexing, notifications).
 *
 * @see com.eshop.app.core.events.listeners.ProductEventListener
 * @since 2.0
 */
@Getter
public class ProductCreatedEvent extends ApplicationEvent {

    private final Product product;

    public ProductCreatedEvent(Object source, Product product) {
        super(source);
        this.product = product;
    }
}
