package com.eshop.app.exception;

/**
 * Exception thrown when unable to generate a unique friendly URL.
 * 
 * @since 2.0
 */
public class FriendlyUrlGenerationException extends RuntimeException {
    
    public FriendlyUrlGenerationException(String message) {
        super(message);
    }
    
    public FriendlyUrlGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
