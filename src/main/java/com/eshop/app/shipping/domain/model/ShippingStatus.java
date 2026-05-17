package com.eshop.app.shipping.domain.model;

public enum ShippingStatus {
    PENDING,           // Shipping label created but not shipped
    SHIPPED,          // Package shipped
    IN_TRANSIT,       // Package in transit
    OUT_FOR_DELIVERY, // Package out for delivery
    DELIVERED,        // Package delivered
    RETURNED,         // Package returned to sender
    LOST,             // Package lost
    DAMAGED           // Package damaged
}
