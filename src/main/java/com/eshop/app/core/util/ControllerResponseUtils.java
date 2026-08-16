package com.eshop.app.core.util;

import com.eshop.app.core.api.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * [HARDEN] Unified controller response utility.
 * Simplifies ResponseEntity creation and ensures consistent ApiResponse wrapping.
 */
public final class ControllerResponseUtils {
    
    private ControllerResponseUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static <T> ResponseEntity<ApiResponse<T>> ok(String message, T data) {
        return ResponseEntity.ok(ApiResponse.success(message, data));
    }

    public static <T> ResponseEntity<ApiResponse<T>> ok(T data) {
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    public static <T> ResponseEntity<ApiResponse<T>> created(String message, T data) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(message, data));
    }
    
    public static <T> ResponseEntity<ApiResponse<T>> noContent() {
        return ResponseEntity.noContent().build();
    }
    
    public static <T> ResponseEntity<ApiResponse<T>> accepted(String message, T data) {
        return ResponseEntity.accepted().body(ApiResponse.success(message, data));
    }
}



