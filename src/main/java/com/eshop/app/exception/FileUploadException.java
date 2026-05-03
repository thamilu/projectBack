package com.eshop.app.exception;

/**
 * Exception thrown when a file upload operation fails.
 * 
 * @author E-Shop Team
 */
public class FileUploadException extends RuntimeException {
    
    public FileUploadException(String message) {
        super(message);
    }
    
    public FileUploadException(String message, Throwable cause) {
        super(message, cause);
    }
}
