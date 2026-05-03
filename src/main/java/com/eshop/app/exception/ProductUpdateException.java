package com.eshop.app.exception;

import lombok.Getter;

/**
 * Exception thrown when product update operation fails.
 * 
 * <p>This exception covers various update failure scenarios:
 * <ul>
 *   <li>Validation failures</li>
 *   <li>Business rule violations</li>
 *   <li>Database constraint violations</li>
 *   <li>Concurrent modification conflicts</li>
 * </ul>
 * 
 * <p>HTTP Status: 409 CONFLICT or 400 BAD REQUEST
 * 
 * @since 1.0
 */
@Getter
public class ProductUpdateException extends RuntimeException {
    
    private final Long productId;
    private final String reason;
    
    /**
     * Construct exception with product ID and message.
     * 
     * @param productId the product ID
     * @param message the error message
     */
    public ProductUpdateException(Long productId, String message) {
        super(String.format("Failed to update product %d: %s", productId, message));
        this.productId = productId;
        this.reason = message;
    }
    
    /**
     * Construct exception with product ID, message, and cause.
     * 
     * @param productId the product ID
     * @param message the error message
     * @param cause the underlying cause
     */
    public ProductUpdateException(Long productId, String message, Throwable cause) {
        super(String.format("Failed to update product %d: %s", productId, message), cause);
        this.productId = productId;
        this.reason = message;
    }
}
