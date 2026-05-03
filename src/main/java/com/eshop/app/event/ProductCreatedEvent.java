package com.eshop.app.event;

import com.eshop.app.entity.Product;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Event published when a new product is created.
 * 
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
