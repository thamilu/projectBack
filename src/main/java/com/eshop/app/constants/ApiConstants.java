package com.eshop.app.constants;

import java.util.*;
import java.util.regex.Pattern;

/**
 * API Constants for EShop Application.
 * Immutable configuration values for API metadata and endpoints.
 * 
 * @author EShop Team
 * @version 1.0.0
 * @since 2024-01-01
 */
public final class ApiConstants {
    
    private ApiConstants() { 
        throw new UnsupportedOperationException(
            "Cannot instantiate utility class: " + getClass().getName()
        ); 
    }

    // ==================== API METADATA ====================
    public static final String API_NAME = "EShop API";
    public static final String API_DESCRIPTION = 
        "Spring Boot E-commerce REST API with Multi-Vendor Support";
    // Default API version (can be externalized to properties)
    public static final String API_VERSION = "1.0.0";
    // Welcome message (used by home controller)
    public static final String WELCOME_MESSAGE = Messages.WELCOME;
    
    // ==================== API PATHS ====================
    public static final String API_PREFIX = "/api";
    public static final String API_VERSION_PATH = "/v1";
    public static final String BASE_PATH = API_PREFIX + API_VERSION_PATH;
    
    // ==================== DOCUMENTATION ====================
    public static final String SWAGGER_UI_PATH = "/swagger-ui.html";
    public static final String API_DOCS_PATH = "/v3/api-docs";
    // Documentation URL (relative)
    public static final String DOCUMENTATION_URL = SWAGGER_UI_PATH;
    
    // ==================== ENDPOINTS ====================
    public static final class Endpoints {
        private Endpoints() { 
            throw new UnsupportedOperationException("Cannot instantiate"); 
        }
        
        public static final String PRODUCTS = BASE_PATH + "/products";
        public static final String PRODUCT_IMAGES = BASE_PATH + "/productImages";
        public static final String PRODUCT_LOCATION = BASE_PATH + "/location";
        public static final String BRANDS = BASE_PATH + "/brands";
        public static final String CATEGORIES = BASE_PATH + "/categories";
        public static final String STORES = BASE_PATH + "/stores";
        public static final String USERS = BASE_PATH + "/users";
        public static final String CART = BASE_PATH + "/cart";
        public static final String ORDERS = BASE_PATH + "/orders";
        public static final String PAYMENTS = BASE_PATH + "/payments";
        public static final String PAYMENT_METHODS = BASE_PATH + "/paymentMethods";
        public static final String AUTH = BASE_PATH + "/auth";
        public static final String HOME = BASE_PATH + "/home";
        public static final String DASHBOARD = BASE_PATH + "/dashboard";
        public static final String HEALTH = BASE_PATH + "/health";
        public static final String ADMIN_CATEGORY = BASE_PATH + "/admin/categories";
        public static final String COUPONS = BASE_PATH + "/coupons";
        public static final String WEBHOOKS_PAYMENT = BASE_PATH + "/webhooks/payment";
        public static final String WEBHOOKS_ORDER = BASE_PATH + "/webhooks/order";
        public static final String ADMIN_PROBE = BASE_PATH + "/admin/probe";
        public static final String SEARCH = BASE_PATH + "/search";
        public static final String PRODUCT_REVIEWS = BASE_PATH + "/productReviews";    
        public static final String PROFILE = BASE_PATH + "/profile";
        public static final String PARSECONTROLLER = BASE_PATH + "/parse";
        public static final String PAYMENT_ANALYTICS = BASE_PATH + "/paymentAnalytics";
        public static final String SELLER_CATEGORY = BASE_PATH + "/seller/categories";
        public static final String SHIPPING = BASE_PATH + "/shipping";
        public static final String SHOPPING_CART = BASE_PATH + "/shoppingCart";
        // public static final String WISHLIST = BASE_PATH + "/wishlist";
        // public static final String TestAdminProbe = BASE_PATH + "/admin/probe";
        

        // Path variables
        public static final String ID_PATH = "/{id}";
        public static final String SLUG_PATH = "/{slug}";
        
        public static Map<String, String> getAll() {
            return Map.ofEntries(
                Map.entry("products", PRODUCTS),
                Map.entry("brands", BRANDS),
                Map.entry("categories", CATEGORIES),
                Map.entry("stores", STORES),
                Map.entry("users", USERS),
                Map.entry("cart", CART),
                Map.entry("orders", ORDERS),
                Map.entry("payments", PAYMENTS),
                Map.entry("auth", AUTH),
                Map.entry("home", HOME),
                Map.entry("dashboard", DASHBOARD),
                Map.entry("health", HEALTH),
                Map.entry("adminCategory", ADMIN_CATEGORY),
                Map.entry("coupons", COUPONS),
                Map.entry("webhooksPayment", WEBHOOKS_PAYMENT),
                Map.entry("webhooksOrder", WEBHOOKS_ORDER),
                Map.entry("adminProbe", ADMIN_PROBE)
               
            );
        }
    }

    /**
     * Backwards-compatible view of endpoints and features used by controllers.
     * Prefer `Endpoints` class directly in new code.
     */
    public static final Map<String, String> ENDPOINTS = Endpoints.getAll();

    public static final Map<String, String> FEATURES = Map.ofEntries(
        Map.entry("search", "Full text search with filters"),
        Map.entry("multiVendor", "Multiple sellers per platform"),
        Map.entry("payments", "Stripe and Razorpay integrations"),
        Map.entry("reviews", "Product reviews and ratings")
    );

    // ==================== ERROR CODES ====================
    public static final class ErrorCodes {
        private ErrorCodes() { throw new UnsupportedOperationException(); }
        public static final String VALIDATION_ERROR = "ERR_VALIDATION";
        public static final String AUTH_FAILED = "ERR_AUTH_FAILED";
        public static final String TOKEN_EXPIRED = "ERR_TOKEN_EXPIRED";
        public static final String RESOURCE_NOT_FOUND = "ERR_NOT_FOUND";
        public static final String DUPLICATE_RESOURCE = "ERR_DUPLICATE";
        public static final String INSUFFICIENT_STOCK = "ERR_STOCK";
        public static final String PAYMENT_FAILED = "ERR_PAYMENT";
        public static final String RATE_LIMITED = "ERR_RATE_LIMIT";
    }

    // ==================== MEDIA TYPES ====================
    public static final class MediaTypes {
        private MediaTypes() { throw new UnsupportedOperationException(); }
        public static final String JSON = "application/json";
        public static final String JSON_UTF8 = "application/json;charset=UTF-8";
        public static final String MULTIPART = "multipart/form-data";
        public static final String PDF = "application/pdf";
    }

    // ==================== TIMEOUTS ====================
    public static final class Timeouts {
        private Timeouts() { throw new UnsupportedOperationException(); }
        public static final int CONNECTION_TIMEOUT_MS = 5000;
        public static final int READ_TIMEOUT_MS = 30000;
        public static final int WRITE_TIMEOUT_MS = 30000;
        public static final long SESSION_TIMEOUT_MINUTES = 30L;
        public static final long JWT_EXPIRY_HOURS = 24L;
        public static final long REFRESH_TOKEN_EXPIRY_DAYS = 7L;
    }

    // ==================== FILE UPLOAD ====================
    public static final class FileUpload {
        private FileUpload() { throw new UnsupportedOperationException(); }
        public static final long MAX_FILE_SIZE = 10L * 1024 * 1024; // 10MB
        public static final long MAX_REQUEST_SIZE = 50L * 1024 * 1024; // 50MB
        public static final Set<String> ALLOWED_IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/gif", "image/webp");
        public static final Set<String> ALLOWED_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".gif", ".webp");
    }

    // ==================== REGEX PATTERNS ====================
    public static final class Patterns {
        private Patterns() { throw new UnsupportedOperationException(); }
        public static final Pattern UUID = Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
        public static final Pattern SLUG = Pattern.compile("^[a-z0-9]+(?:-[a-z0-9]+)*$");
        public static final Pattern SKU = Pattern.compile("^[A-Z0-9]{3,20}$");
    }

    
    // ==================== MESSAGES ====================
    public static final class Messages {
        private Messages() { 
            throw new UnsupportedOperationException("Cannot instantiate"); 
        }
        
        public static final String WELCOME = "Welcome to EShop API";
        public static final String UNAUTHORIZED = 
            "Authentication required to access this resource";
        public static final String FORBIDDEN = 
            "You don't have permission to access this resource";
        public static final String NOT_FOUND = "Resource not found";
        public static final String INTERNAL_ERROR = 
            "An unexpected error occurred. Please try again later";
        public static final String VALIDATION_ERROR = 
            "Validation failed for the request";
    }
    
    // ==================== PAGINATION ====================
    public static final class Pagination {
        private Pagination() { 
            throw new UnsupportedOperationException("Cannot instantiate"); 
        }
        
        public static final int DEFAULT_PAGE = 0;
        public static final int DEFAULT_SIZE = 20;
        public static final int MAX_SIZE = 100;
        public static final String DEFAULT_SORT = "createdAt";
        public static final String SORT_DESC = "desc";
        public static final String SORT_ASC = "asc";
    }
    
    // ==================== HEADERS ====================
    public static final class Headers {
        private Headers() { 
            throw new UnsupportedOperationException("Cannot instantiate"); 
        }
        
        public static final String AUTHORIZATION = "Authorization";
        public static final String BEARER_PREFIX = "Bearer ";
        public static final String X_REQUEST_ID = "X-Request-ID";
        public static final String X_CORRELATION_ID = "X-Correlation-ID";
        public static final String X_RATE_LIMIT_REMAINING = "X-Rate-Limit-Remaining";
        public static final String X_RATE_LIMIT_RESET = "X-Rate-Limit-Reset";
    }
    
    // ==================== CACHE ====================
    public static final class Cache {
        private Cache() { 
            throw new UnsupportedOperationException("Cannot instantiate"); 
        }
        
        public static final String PRODUCTS_CACHE = "products";
        public static final String CATEGORIES_CACHE = "categories";
        public static final String BRANDS_CACHE = "brands";
        public static final long DEFAULT_TTL_SECONDS = 3600L; // 1 hour
        public static final long SHORT_TTL_SECONDS = 300L;    // 5 minutes
        public static final int MAX_CACHE_SIZE = 1000;
    }
    
    // ==================== VALIDATION ====================
    public static final class Validation {
        private Validation() { 
            throw new UnsupportedOperationException("Cannot instantiate"); 
        }
        
        public static final int MIN_PASSWORD_LENGTH = 8;
        public static final int MAX_PASSWORD_LENGTH = 128;
        public static final int MIN_USERNAME_LENGTH = 3;
        public static final int MAX_USERNAME_LENGTH = 50;
        public static final int MAX_EMAIL_LENGTH = 255;
        public static final String EMAIL_REGEX = 
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
        public static final String PHONE_REGEX = 
            "^\\+?[1-9]\\d{1,14}$";
    }
    
    // ==================== RATE LIMITING ====================
    public static final class RateLimit {
        private RateLimit() { 
            throw new UnsupportedOperationException("Cannot instantiate"); 
        }
        
        public static final int DEFAULT_REQUESTS_PER_MINUTE = 60;
        public static final int AUTH_REQUESTS_PER_MINUTE = 10;
        public static final int SEARCH_REQUESTS_PER_MINUTE = 30;
    }
}
