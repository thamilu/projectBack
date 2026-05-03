package com.eshop.app.entity.enums;

/**
 * Product types determining behavior and features.
 */
public enum ProductType {
    /** Simple product with no variants */
    SIMPLE,
    /** Master product with variants */
    CONFIGURABLE,
    /** Variant of a configurable product */
    VARIANT,
    /** Bundle of multiple products */
    BUNDLE,
    /** Grouped products sold together */
    GROUPED,
    /** Digital/downloadable product */
    DIGITAL,
    /** Subscription/recurring product */
    SUBSCRIPTION,
    /** Virtual product (services, etc.) */
    VIRTUAL,
    /** Gift card product */
    GIFT_CARD
}
