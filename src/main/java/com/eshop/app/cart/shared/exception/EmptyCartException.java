package com.eshop.app.cart.shared.exception;





public class EmptyCartException extends RuntimeException {
    public EmptyCartException(String message) {
        super(message);
    }
}
