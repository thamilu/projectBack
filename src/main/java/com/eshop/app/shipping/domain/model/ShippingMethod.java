package com.eshop.app.shipping.domain.model;

public enum ShippingMethod {
    STANDARD,      // 5-7 business days
    EXPEDITED,     // 2-3 business days  
    OVERNIGHT,     // Next business day
    TWO_DAY,       // 2 business days
    SAME_DAY,      // Same day delivery
    PICKUP         // Customer pickup
}
