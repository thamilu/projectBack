package com.eshop.app.core.infrastructure.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.*;
import java.util.List;

/**
 * Application-wide configuration properties.
 * Binds all app.* properties from application.properties
 * 
 * @author E-Shop Team
 * @version 1.0.0
 */
@Configuration
@ConfigurationProperties(prefix = "app")
@Data
@Validated
public class AppProperties {

    private String name = "E-Shop";
    private String version = "1.0.0";
    private String environment = "development";
    private boolean debug = false;

    private String backendUrl;
    private Security security = new Security();
    private Cors cors = new Cors();
    private Analytics analytics = new Analytics();
    private RateLimit ratelimit = new RateLimit();
    private Logging logging = new Logging();
    private Audit audit = new Audit();
    private Storage storage = new Storage();
    private Validation validation = new Validation();
    private Api api = new Api();
    private Business business = new Business();
    private Performance performance = new Performance();
    private Features features = new Features();
    private Product product = new Product();
    private Cache cache = new Cache();

    /**
     * Security configuration
     */
    @Data
    public static class Security {
        private Roles roles = new Roles();

        private String rolePrefix = "ROLE_";
        private String claimRealms = "realm_access";
        private String claimRoles = "roles";

        private String defaultRedirectUri;

        private List<String> allowedRedirectUris;
        private Headers headers = new Headers();

        /** Defense-in-depth ceiling on bulk user-management operations (activate/
         *  deactivate), enforced at the service layer independent of any per-request
         *  page-size limit enforced at the API boundary (e.g. UserController's
         *  {@code @Size(max = 100)} on its bulk endpoints). Externalized so it can be
         *  tuned per environment without a code change. */
        private int bulkOperationMaxSize = 1000;

        @Data
        public static class Roles {
            private String admin = "ADMIN";
            private String seller = "SELLER";
            private String customer = "CUSTOMER";
            private String delivery = "DELIVERY_AGENT";
        }

        @Data
        public static class Headers {
            private boolean enabled = true;
            private String contentSecurityPolicy = "default-src 'self'";
            private String xFrameOptions = "DENY";
            private String xContentTypeOptions = "nosniff";
            private String xXssProtection = "1; mode=block";
            private String strictTransportSecurity = "max-age=31536000; includeSubDomains";
            private String referrerPolicy = "no-referrer";
        }

    }

    /**
     * CORS configuration
     */
    @Data
    public static class Cors {
        private boolean enabled = true;
        private String allowedOrigins;
        private String allowedMethods = "GET,POST,PUT,DELETE,PATCH,OPTIONS";
        private String allowedHeaders = "Authorization,Content-Type,X-Requested-With,Accept,Origin,X-API-Version,X-Request-ID";
        private String exposedHeaders = "X-Total-Count,X-Page-Number,X-Page-Size";
        private boolean allowCredentials = true;
        private long maxAge = 3600;
    }

    /**
     * Analytics configuration
     */
    @Data
    public static class Analytics {
        @Min(1)
        @Max(300)
        private int timeoutSeconds = 30;
        
        @Min(1)
        @Max(100)
        private int defaultTopProductsLimit = 10;
        
        @Min(1)
        @Max(365)
        private int maxDaysRange = 365;
    }

    /**
     * Rate limiting configuration
     */
    @Data
    public static class RateLimit {
        private boolean enabled = true;
        
        @Min(1)
        private int standardRequestsPerMinute = 100;
        
        @Min(1)
        private int premiumRequestsPerMinute = 1000;
        
        @Min(1)
        private int analyticsRequestsPerMinute = 20;
    }

    /**
     * Logging configuration
     */
    @Data
    public static class Logging {
        private Request request = new Request();

        @Data
        public static class Request {
            private boolean enabled = true;
            private boolean includeQueryString = true;
            private boolean includeClientInfo = true;
            private boolean includeHeaders = false;
            private boolean includePayload = false;
            private int maxPayloadLength = 1000;
        }
    }

    /**
     * Audit logging configuration
     */
    @Data
    public static class Audit {
        private boolean enabled = true;
        private boolean logReads = false;
        private boolean logCreates = true;
        private boolean logUpdates = true;
        private boolean logDeletes = true;
        private boolean async = true;
    }

    /**
     * Storage configuration (app.storage.*)
     */
    @Data
    public static class Storage {
        private long maxFileSize = 5242880; // 5MB in bytes
        private int maxFiles = 10;
        private int maxFilenameLength = 255;
        private String allowedMimeTypes = "image/jpeg,image/png,image/webp";
        private String allowedExtensions = "jpg,jpeg,png,webp";
        private int maxImageWidth = 1920;
        private int maxImageHeight = 1080;
        private float compressQuality = 0.85f;
        private boolean virusScanEnabled = false;
        
        private R2 r2 = new R2();

        @Data
        public static class R2 {
            private String accessKeyId;
            private String secretAccessKey;
            private String accountId;
            private String bucketName;
            private String publicUrl;
            private String region = "auto";
        }
    }

    /**
     * Input validation configuration
     */
    @Data
    public static class Validation {
        private int maxStringLength = 5000;
        private int maxCollectionSize = 100;
        private boolean sanitizeHtml = true;
        private boolean allowHtmlTags = false;
    }

    /**
     * API configuration
     */
    @Data
    public static class Api {
        private String version = "v1";
        private String basePath = "/api/v1";
        private String deprecatedVersions = "";
    }

    /**
     * Business rules configuration
     */
    @Data
    public static class Business {
        @PositiveOrZero
        private double defaultCommissionRate = 0.05;

        private String defaultCurrency = "INR";

        @Positive
        private double minPaymentAmount = 0.01;

        @Positive
        private double maxPaymentAmount = 100000.00;

        private String allowedGateways = "STRIPE,PAYPAL,RAZORPAY";

        private String allowedCurrencies = "USD,EUR,INR,GBP";

        private boolean upiEnabled = true;
        private boolean emiEnabled = true;

        @Positive
        private double emiMinAmount = 5000.00;

        private double minProductPrice = 0.01;
        private double maxProductPrice = 999999.99;
        private int maxDiscountPercentage = 99;
        private int maxOrderItems = 100;
        private int orderExpiryMinutes = 30;
        private int cartExpiryDays = 30;
    }

    /**
     * Performance thresholds
     */
    @Data
    public static class Performance {
        private long slowQueryThresholdMs = 1000;
        private long httpTimeoutMs = 30000;
        private long asyncTimeoutMs = 60000;
        private int maxExportRows = 100000;
    }

    /**
     * Feature flags
     */
    @Data
    public static class Features {
        private Analytics analytics = new Analytics();
        private Recommendations recommendations = new Recommendations();
        private Wishlist wishlist = new Wishlist();
        private Reviews reviews = new Reviews();
        private Coupons coupons = new Coupons();
        private Affiliate affiliate = new Affiliate();
        private Subscriptions subscriptions = new Subscriptions();

        @Data
        public static class Analytics {
            private boolean enabled = true;
        }

        @Data
        public static class Recommendations {
            private boolean enabled = true;
        }

        @Data
        public static class Wishlist {
            private boolean enabled = true;
        }

        @Data
        public static class Reviews {
            private boolean enabled = true;
        }

        @Data
        public static class Coupons {
            private boolean enabled = true;
        }

        @Data
        public static class Affiliate {
            private boolean enabled = false;
        }

        @Data
        public static class Subscriptions {
            private boolean enabled = false;
        }
    }

    /**
     * Product service configuration
     */
    @Data
    public static class Product {
        private int lowStockThreshold = 10;
        private int batchSize = 100;
        private int maxBatchSize = 100;
        private int maxFriendlyUrlAttempts = 1000;
        private String statisticsCacheTtl = "15m";
        
        private Search search = new Search();

        @Data
        public static class Search {
            private int defaultPageSize = 20;
            private int maxPageSize = 100;
            private int maxSearchResultsTotal = 10000;
            private int maxPageNumber = 1000;
        }
    }

    /**
     * Cache TTL configuration
     */
    @Data
    public static class Cache {
        private Products products = new Products();
        private Categories categories = new Categories();
        private Dashboard dashboard = new Dashboard();
        private Statistics statistics = new Statistics();
        private Analytics analytics = new Analytics();

        @Data
        public static class Products {
            private int ttl = 3600; // seconds
        }

        @Data
        public static class Categories {
            private int ttl = 7200; // seconds
        }

        @Data
        public static class Dashboard {
            private int ttl = 300; // seconds
        }

        @Data
        public static class Statistics {
            private int ttl = 300; // seconds
        }

        @Data
        public static class Analytics {
            private int ttl = 600; // seconds
        }
    }
}
