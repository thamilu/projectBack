package com.eshop.app.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class InsufficientStockException extends BusinessException {

    private static final long serialVersionUID = 1L;

    private final Long productId;
    private final Integer requestedQuantity;
    private final Integer availableQuantity;

    /**
     * Constructor matching signature used in Product entity.
     * @param message Custom error message
     * @param productId ID of the product with insufficient stock
     * @param availableQuantity Available quantity
     * @param requestedQuantity Requested quantity
     */
    public InsufficientStockException(String message, Long productId, Integer availableQuantity, Integer requestedQuantity) {
        super(message, "INSUFFICIENT_STOCK", HttpStatus.BAD_REQUEST);
        this.productId = productId;
        this.requestedQuantity = requestedQuantity;
        this.availableQuantity = availableQuantity;

        addDetail("productId", productId);
        addDetail("requestedQuantity", requestedQuantity);
        addDetail("availableQuantity", availableQuantity);
    }

    public InsufficientStockException(Long productId, Integer requestedQuantity, Integer availableQuantity) {
        super(
            String.format("Insufficient stock for product %d. Requested: %d, Available: %d",
                productId, requestedQuantity, availableQuantity),
            "INSUFFICIENT_STOCK",
            HttpStatus.BAD_REQUEST
        );
        this.productId = productId;
        this.requestedQuantity = requestedQuantity;
        this.availableQuantity = availableQuantity;

        addDetail("productId", productId);
        addDetail("requestedQuantity", requestedQuantity);
        addDetail("availableQuantity", availableQuantity);
    }

    public InsufficientStockException(String message) {
        super(message, "INSUFFICIENT_STOCK", HttpStatus.BAD_REQUEST);
        this.productId = null;
        this.requestedQuantity = null;
        this.availableQuantity = null;
    }
}
