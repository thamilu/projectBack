package com.eshop.app.entity.enums;

/**
 * Stock availability status.
 */
public enum StockStatus {
    /** Product is in stock */
    IN_STOCK,
    /** Stock is low (at or below reorder level) */
    LOW_STOCK,
    /** Product is out of stock */
    OUT_OF_STOCK,
    /** Product is on backorder */
    ON_BACKORDER,
    /** Pre-order available */
    PRE_ORDER,
    /** Made to order */
    MADE_TO_ORDER
}
