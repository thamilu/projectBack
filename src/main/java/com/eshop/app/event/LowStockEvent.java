package com.eshop.app.event;

import com.eshop.app.entity.Product;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Event published when product stock falls below threshold.
 * 
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
