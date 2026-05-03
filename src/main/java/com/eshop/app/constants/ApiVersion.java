package com.eshop.app.constants;

/**
 * API versioning constants.
 * Centralized version management for all API endpoints.
 * 
 * Usage:
 * - V1: Current stable version
 * - V2: Next version with breaking changes
 * 
 * @author EShop Team
 * @since 2.0
 */
public final class ApiVersion {
    
    private ApiVersion() {
        // Prevent instantiation
    }
    
    public static final String V1 = "/api/v1";
    public static final String V2 = "/api/v2";
    
    public static final String CURRENT = V1;
}
