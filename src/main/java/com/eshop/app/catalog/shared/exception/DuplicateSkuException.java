package com.eshop.app.catalog.shared.exception;

import com.eshop.app.core.exception.business.DuplicateResourceException;
import lombok.Getter;

@Getter
public class DuplicateSkuException extends DuplicateResourceException {

    private final String sku;
    private final Long existingProductId;

    public DuplicateSkuException(String sku) {
        super("Product", "SKU", sku);
        this.sku = sku;
        this.existingProductId = null;
    }

    public DuplicateSkuException(String sku, Long existingProductId) {
        super(String.format("Product with SKU '%s' already exists (Product ID: %d)", sku, existingProductId));
        this.sku = sku;
        this.existingProductId = existingProductId;
    }
}
