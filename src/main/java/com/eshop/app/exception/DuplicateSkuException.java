package com.eshop.app.exception;

import lombok.Getter;

/**
 * Exception thrown when attempting to create a product with duplicate SKU.
 * 
 * <p>SKU (Stock Keeping Unit) must be unique across all products.
 * This exception is thrown during product creation or update when
 * the SKU already exists in the database.
 * 
 * <p>HTTP Status: 409 CONFLICT
 * 
 * @since 1.0
 */
@Getter
public class DuplicateSkuException extends DuplicateResourceException {

    private final String sku;
    private final Long existingProductId;

    /**
     * Construct exception with SKU only.
     *
     * @param sku the duplicate SKU
     */
    public DuplicateSkuException(String sku) {
        super("Product", "SKU", sku);
        this.sku = sku;
        this.existingProductId = null;
    }

    /**
     * Construct exception with SKU and existing product ID.
     *
     * @param sku the duplicate SKU
     * @param existingProductId the ID of product with this SKU
     */
    public DuplicateSkuException(String sku, Long existingProductId) {
        super(String.format("Product with SKU '%s' already exists (Product ID: %d)", sku, existingProductId));
        this.sku = sku;
        this.existingProductId = existingProductId;
    }
}
