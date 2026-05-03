package com.eshop.app.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class InsufficientInventoryException extends BusinessException {

    private static final long serialVersionUID = 1L;

    public InsufficientInventoryException(String message) {
        super(message, "INSUFFICIENT_INVENTORY", HttpStatus.BAD_REQUEST);
    }

    public InsufficientInventoryException(Long productId, Integer requested, Integer available) {
        super(
            String.format("Insufficient inventory for product %d. Requested: %d, Available: %d",
                productId, requested, available),
            "INSUFFICIENT_INVENTORY",
            HttpStatus.BAD_REQUEST
        );
        addDetail("productId", productId);
        addDetail("requestedQuantity", requested);
        addDetail("availableQuantity", available);
    }
}
