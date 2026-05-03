package com.eshop.app.exception;

import lombok.Getter;

/**
 * Exception thrown when a product is not found.
 * 
 * <p>This exception is thrown when attempting to access a product that
 * does not exist in the database by ID, SKU, or friendly URL.
 * 
 * <p>HTTP Status: 404 NOT FOUND
 * 
 * @since 1.0
 */
@Getter
public class ProductNotFoundException extends ResourceNotFoundException {
    
    
    /**
     * Product identifier that was not found (ID, SKU, or URL).
     */
    private final String identifier;
    
    /**
     * Type of identifier used in search.
     */
    private final IdentifierType identifierType;
    
    /**
     * Identifier type enumeration.
     */
    public enum IdentifierType {
        ID,
        SKU,
        FRIENDLY_URL
    }
    
    /**
     * Construct exception with product ID.
     * 
     * @param productId the product ID that was not found
     */
    public ProductNotFoundException(Long productId) {
        super(String.format("Product not found with id: %d", productId));
        this.identifier = String.valueOf(productId);
        this.identifierType = IdentifierType.ID;
    }
    
    /**
     * Construct exception with identifier and type.
     * 
     * @param identifier the identifier value
     * @param type the type of identifier
     */
    public ProductNotFoundException(String identifier, IdentifierType type) {
        super(String.format("Product not found with %s: %s", 
            type.name().toLowerCase().replace('_', ' '), 
            identifier));
        this.identifier = identifier;
        this.identifierType = type;
    }
    
    /**
     * Construct exception with custom message.
     * 
     * @param message the custom error message
     */
    public ProductNotFoundException(String message) {
        super(message);
        this.identifier = null;
        this.identifierType = null;
    }
}
