package com.eshop.app.exception;

/**
 * Exception thrown when stock update fails.
 * 
 * @since 2.0
 */
public class StockUpdateException extends RuntimeException {
    
    public StockUpdateException(String message) {
        super(message);
    }
    
    public StockUpdateException(String message, Throwable cause) {
        super(message, cause);
    }
}
