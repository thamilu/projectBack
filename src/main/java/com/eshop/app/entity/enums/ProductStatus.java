package com.eshop.app.entity.enums;

/**
 * Product lifecycle status.
 */
public enum ProductStatus {
    /** Product is being created */
    DRAFT,
    /** Product is awaiting approval */
    PENDING_REVIEW,
    /** Product is live and purchasable */
    ACTIVE,
    /** Product is temporarily hidden */
    INACTIVE,
    /** Product has been discontinued */
    DISCONTINUED,
    /** Product is archived */
    ARCHIVED,
    /** Product is out of season */
    OUT_OF_SEASON,
    /** Product is coming soon */
    COMING_SOON
}
