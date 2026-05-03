package com.eshop.app.dto.request;

/**
 * Stock update operation types.
 */
public enum StockOperation {
    /**
     * Set stock to exact quantity.
     */
    SET,
    
    /**
     * Add quantity to current stock.
     */
    INCREMENT,
    
    /**
     * Subtract quantity from current stock.
     */
    DECREMENT
}
