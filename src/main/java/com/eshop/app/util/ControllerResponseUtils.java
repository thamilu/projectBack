package com.eshop.app.util;

import com.eshop.app.dto.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Utility class for consistent API response building across controllers
 * 
 * Centralizes ResponseEntity construction logic to follow DRY principle
 * Ensures consistent response format, status codes, and headers across the application
 * 
 * @author E-Shop Team
 * @version 1.0
 */
public final class ControllerResponseUtils {
    
    private ControllerResponseUtils() {
        // Utility class - prevent instantiation
    }
    
    // ==================== SUCCESS RESPONSES ====================
    
    /**
     * Build a successful 200 OK response with data
     * 
     * @param data Response data
     * @param <T> Type of response data
     * @return ResponseEntity with 200 OK status
     */
    public static <T> ResponseEntity<ApiResponse<T>> ok(T data) {
        return ResponseEntity.ok(ApiResponse.success(data));
    }
    
    /**
     * Build a successful 200 OK response with data and custom message
     * 
     * @param message Custom success message
     * @param data Response data
     * @param <T> Type of response data
     * @return ResponseEntity with 200 OK status
     */
    public static <T> ResponseEntity<ApiResponse<T>> ok(String message, T data) {
        return ResponseEntity.ok(ApiResponse.success(message, data));
    }
    
    /**
     * Build a successful 201 CREATED response with data
     * Automatically constructs Location header from the created resource
     * 
     * @param data Response data
     * @param <T> Type of response data
     * @return ResponseEntity with 201 CREATED status
     */
    public static <T> ResponseEntity<ApiResponse<T>> created(T data) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Resource created successfully", data));
    }
    
    /**
     * Build a successful 201 CREATED response with custom message and data
     * 
     * @param message Custom success message
     * @param data Response data
     * @param <T> Type of response data
     * @return ResponseEntity with 201 CREATED status
     */
    public static <T> ResponseEntity<ApiResponse<T>> created(String message, T data) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(message, data));
    }
    
    /**
     * Build a successful 202 ACCEPTED response for asynchronous operations
     * 
     * @param message Status message
     * @param data Response data
     * @param <T> Type of response data
     * @return ResponseEntity with 202 ACCEPTED status
     */
    public static <T> ResponseEntity<ApiResponse<T>> accepted(String message, T data) {
        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(message, data));
    }
    
    /**
     * Build a successful 204 NO CONTENT response
     * Used for successful operations that return no data (e.g., DELETE)
     * 
     * @return ResponseEntity with 204 NO CONTENT status
     */
    public static <T> ResponseEntity<ApiResponse<T>> noContent() {
        return ResponseEntity.noContent().build();
    }
    
    // ==================== ERROR RESPONSES ====================
    
    /**
     * Build a 400 BAD REQUEST error response
     * 
     * @param message Error message
     * @return ResponseEntity with 400 status
     */
    public static <T> ResponseEntity<ApiResponse<T>> badRequest(String message) {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(message, null));
    }
    
    /**
     * Build a 400 BAD REQUEST error response with details
     * 
     * @param message Error message
     * @param details Additional error details
     * @return ResponseEntity with 400 status
     */
    public static <T> ResponseEntity<ApiResponse<T>> badRequest(String message, Object details) {
        // Delegate to the Object overload to avoid type mismatch
        @SuppressWarnings("unchecked")
        ResponseEntity<ApiResponse<T>> response = (ResponseEntity<ApiResponse<T>>) (ResponseEntity<?>) badRequestObject(message, details);
        return response;
    }

    /**
     * Build a 400 BAD REQUEST error response with details (Object version)
     *
     * @param message Error message
     * @param details Additional error details
     * @return ResponseEntity with 400 status
     */
    public static ResponseEntity<ApiResponse<Object>> badRequestObject(String message, Object details) {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(message, details));
    }
    
    /**
     * Build a 401 UNAUTHORIZED error response
     * 
     * @param message Error message (e.g., "Invalid credentials", "Token expired")
     * @return ResponseEntity with 401 status
     */
    public static <T> ResponseEntity<ApiResponse<T>> unauthorized(String message) {
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(ApiResponse.error(message, null));
    }
    
    /**
     * Build a 403 FORBIDDEN error response
     * Used when user is authenticated but lacks permission
     * 
     * @param message Error message
     * @return ResponseEntity with 403 status
     */
    public static <T> ResponseEntity<ApiResponse<T>> forbidden(String message) {
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(ApiResponse.error(message, null));
    }
    
    /**
     * Build a 404 NOT FOUND error response
     * 
     * @param message Error message (e.g., "Resource not found", "Order not found")
     * @return ResponseEntity with 404 status
     */
    public static <T> ResponseEntity<ApiResponse<T>> notFound(String message) {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error(message, null));
    }
    
    /**
     * Build a 409 CONFLICT error response
     * Used for duplicate resources, state conflicts, etc.
     * 
     * @param message Error message (e.g., "Resource already exists", "Order status cannot change")
     * @return ResponseEntity with 409 status
     */
    public static <T> ResponseEntity<ApiResponse<T>> conflict(String message) {
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(ApiResponse.error(message, null));
    }
    
    /**
     * Build a 422 UNPROCESSABLE ENTITY error response
     * Used for validation failures
     * 
     * @param message Error message
     * @param details Validation error details
     * @return ResponseEntity with 422 status
     */
    public static <T> ResponseEntity<ApiResponse<T>> unprocessableEntity(String message, Object details) {
        // Delegate to the Object overload to avoid type mismatch
        @SuppressWarnings("unchecked")
        ResponseEntity<ApiResponse<T>> response = (ResponseEntity<ApiResponse<T>>) (ResponseEntity<?>) unprocessableEntityObject(message, details);
        return response;
    }

    /**
     * Build a 422 UNPROCESSABLE ENTITY error response (Object version)
     *
     * @param message Error message
     * @param details Validation error details
     * @return ResponseEntity with 422 status
     */
    public static ResponseEntity<ApiResponse<Object>> unprocessableEntityObject(String message, Object details) {
        return ResponseEntity
            .status(HttpStatus.valueOf(422))
            .body(ApiResponse.error(message, details));
    }
    
    /**
     * Build a 500 INTERNAL SERVER ERROR response
     * 
     * @param message Error message
     * @return ResponseEntity with 500 status
     */
    public static <T> ResponseEntity<ApiResponse<T>> internalServerError(String message) {
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error(message, null));
    }
    
    /**
     * Build a 503 SERVICE UNAVAILABLE error response
     * 
     * @param message Error message (e.g., "Service temporarily unavailable")
     * @return ResponseEntity with 503 status
     */
    public static <T> ResponseEntity<ApiResponse<T>> serviceUnavailable(String message) {
        return ResponseEntity
            .status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(ApiResponse.error(message, null));
    }
}
