package com.eshop.app.util;

import com.eshop.app.exception.*;
import lombok.extern.slf4j.Slf4j;

/**
 * Utility class for consistent exception handling across services
 * 
 * Centralizes exception creation and logging to follow DRY principle
 * Ensures consistent error messages, logging levels, and exception types
 * 
 * @author E-Shop Team
 * @version 1.0
 */
@Slf4j
public final class ExceptionHandlingUtils {
    
    private ExceptionHandlingUtils() {
        // Utility class - prevent instantiation
    }
    
    // ==================== RESOURCE NOT FOUND ====================
    
    /**
     * Throw ResourceNotFoundException with logging
     * 
     * @param resourceType Type of resource (e.g., "Product", "Order", "User")
     * @param identifier Resource identifier (e.g., "id", "email")
     * @param value Identifier value
     * @throws ResourceNotFoundException Always thrown
     */
    public static void throwResourceNotFound(String resourceType, String identifier, Object value) {
        String message = String.format("%s not found with %s: %s", resourceType, identifier, value);
        log.warn("Resource not found: {}", message);
        throw new ResourceNotFoundException(message);
    }
    
    /**
     * Throw ResourceNotFoundException with custom message
     * 
     * @param message Custom error message
     * @throws ResourceNotFoundException Always thrown
     */
    public static void throwResourceNotFound(String message) {
        log.warn("Resource not found: {}", message);
        throw new ResourceNotFoundException(message);
    }
    
    /**
     * Throw ResourceNotFoundException if object is null
     * 
     * @param object Object to check
     * @param resourceType Resource type for error message
     * @throws ResourceNotFoundException if object is null
     */
    public static void throwIfNull(Object object, String resourceType) {
        if (object == null) {
            throwResourceNotFound(resourceType + " not found");
        }
    }
    
    // ==================== RESOURCE ALREADY EXISTS ====================
    
    /**
     * Throw ResourceAlreadyExistsException with logging
     * 
     * @param resourceType Type of resource
     * @param field Field name (e.g., "email", "username")
     * @param value Field value
     * @throws ResourceAlreadyExistsException Always thrown
     */
    public static void throwResourceAlreadyExists(String resourceType, String field, Object value) {
        String message = String.format("%s with %s '%s' already exists", resourceType, field, value);
        log.warn("Resource already exists: {}", message);
        throw new ResourceAlreadyExistsException(message);
    }
    
    /**
     * Throw ResourceAlreadyExistsException with custom message
     * 
     * @param message Custom error message
     * @throws ResourceAlreadyExistsException Always thrown
     */
    public static void throwResourceAlreadyExists(String message) {
        log.warn("Resource already exists: {}", message);
        throw new ResourceAlreadyExistsException(message);
    }
    
    // ==================== INVALID PARAMETERS ====================
    
    /**
     * Throw InvalidParameterException with logging
     * 
     * @param paramName Parameter name
     * @param reason Reason why parameter is invalid
     * @throws InvalidParameterException Always thrown
     */
    public static void throwInvalidParameter(String paramName, String reason) {
        String message = String.format("Invalid parameter '%s': %s", paramName, reason);
        log.warn("Invalid parameter: {}", message);
        throw new InvalidParameterException(message);
    }
    
    /**
     * Throw InvalidParameterException with custom message
     * 
     * @param message Custom error message
     * @throws InvalidParameterException Always thrown
     */
    public static void throwInvalidParameter(String message) {
        log.warn("Invalid parameter: {}", message);
        throw new InvalidParameterException(message);
    }
    
    // ==================== BUSINESS LOGIC EXCEPTIONS ====================
    
    /**
     * Throw InsufficientStockException
     * 
     * @param productName Product name
     * @param requested Requested quantity
     * @param available Available quantity
     * @throws InsufficientStockException Always thrown
     */
    public static void throwInsufficientStock(String productName, int requested, int available) {
        String message = String.format("Insufficient stock for '%s'. Requested: %d, Available: %d", 
                productName, requested, available);
        log.warn("Insufficient stock: {}", message);
        throw new InsufficientStockException(message);
    }
    
    /**
     * Throw EmptyCartException
     * 
     * @param message Error message
     * @throws EmptyCartException Always thrown
     */
    public static void throwEmptyCart(String message) {
        log.warn("Empty cart operation: {}", message);
        throw new EmptyCartException(message);
    }
    
    // ==================== GENERIC EXCEPTION HANDLING ====================
    
    /**
     * Log and rethrow unexpected exception
     * 
     * @param ex Exception to log and rethrow
     * @param context Additional context for logging
     * @throws RuntimeException Always thrown (rethrows original exception)
     */
    public static void logAndRethrow(Exception ex, String context) {
        log.error("Unexpected error in {}: {}", context, ex.getMessage(), ex);
        if (ex instanceof RuntimeException) {
            throw (RuntimeException) ex;
        }
        throw new RuntimeException("Unexpected error: " + ex.getMessage(), ex);
    }
    
    /**
     * Handle exception based on type
     * 
     * @param ex Exception to handle
     * @return Appropriate error message based on exception type
     */
    public static String getErrorMessage(Exception ex) {
        if (ex instanceof ResourceNotFoundException) {
            return "The requested resource was not found";
        } else if (ex instanceof ResourceAlreadyExistsException) {
            return "The resource already exists";
        } else if (ex instanceof InvalidParameterException) {
            return "One or more parameters are invalid";
        } else if (ex instanceof InsufficientStockException) {
            return "Insufficient stock available";
        } else if (ex instanceof EmptyCartException) {
            return "Shopping cart is empty";
        } else {
            return "An unexpected error occurred";
        }
    }
    
    /**
     * Validate condition and throw exception if false
     * 
     * @param condition Condition to check
     * @param exceptionSupplier Supplier that creates the exception to throw
     * @param message Message for logging
     * @throws RuntimeException Thrown by supplier if condition is false
     */
    public static void validateCondition(boolean condition, 
            java.util.function.Supplier<RuntimeException> exceptionSupplier,
            String message) {
        if (!condition) {
            log.warn("Validation failed: {}", message);
            throw exceptionSupplier.get();
        }
    }
}
