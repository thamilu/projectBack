package com.eshop.app.enums;

/**
 * Enumeration for audit log actions
 */
public enum AuditAction {
    USER_LOGIN("User logged in"),
    USER_LOGOUT("User logged out"),
    USER_REGISTER("User registered"),
    USER_UPDATE("User profile updated"),
    USER_DELETE("User deleted"),

    PRODUCT_CREATE("Product created"),
    PRODUCT_UPDATE("Product updated"),
    PRODUCT_DELETE("Product deleted"),
    PRODUCT_VIEW("Product viewed"),

    ORDER_CREATE("Order created"),
    ORDER_UPDATE("Order updated"),
    ORDER_CANCEL("Order cancelled"),
    ORDER_COMPLETE("Order completed"),
    ORDER_REFUND("Order refunded"),

    CART_ADD_ITEM("Item added to cart"),
    CART_REMOVE_ITEM("Item removed from cart"),
    CART_UPDATE_ITEM("Cart item updated"),
    CART_CLEAR("Cart cleared"),

    PAYMENT_INITIATE("Payment initiated"),
    PAYMENT_COMPLETE("Payment completed"),
    PAYMENT_FAIL("Payment failed"),
    PAYMENT_REFUND("Payment refunded"),

    ADMIN_USER_CREATE("Admin created user"),
    ADMIN_USER_UPDATE("Admin updated user"),
    ADMIN_USER_DELETE("Admin deleted user"),
    ADMIN_ROLE_ASSIGN("Admin assigned role"),

    SYSTEM_CONFIG_UPDATE("System configuration updated"),
    DATA_EXPORT("Data exported"),
    DATA_IMPORT("Data imported");

    private final String displayName;

    AuditAction(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return this.displayName;
    }

}
