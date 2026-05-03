package com.eshop.app.exception;

import lombok.Getter;

/**
 * Exception thrown when product deletion operation fails.
 * 
 * <p>Common scenarios:
 * <ul>
 *   <li>Product has active orders</li>
 *   <li>Product has pending shipments</li>
 *   <li>Product is in active wishlists</li>
 *   <li>Database integrity constraints</li>
 * </ul>
 * 
 * <p>HTTP Status: 409 CONFLICT
 * 
 * @since 1.0
 */
@Getter
public class ProductDeletionException extends RuntimeException {
    
    private final Long productId;
    private final String reason;
    
    /**
     * Construct exception with product ID and reason.
     * 
     * @param productId the product ID
     * @param reason the reason deletion failed
     */
    public ProductDeletionException(Long productId, String reason) {
        super(String.format("Cannot delete product %d: %s", productId, reason));
        this.productId = productId;
        this.reason = reason;
    }
    
    /**
     * Construct exception with product ID, reason, and cause.
     * 
     * @param productId the product ID
     * @param reason the reason deletion failed
     * @param cause the underlying cause
     */
    public ProductDeletionException(Long productId, String reason, Throwable cause) {
        super(String.format("Cannot delete product %d: %s", productId, reason), cause);
        this.productId = productId;
        this.reason = reason;
    }
}
