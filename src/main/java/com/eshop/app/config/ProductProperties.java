package com.eshop.app.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;

/**
 * Externalized configuration properties for Product Service.
 * Provides type-safe configuration with validation.
 * 
 * @since 2.0
 */
@Configuration
@ConfigurationProperties(prefix = "app.product")
@Validated
@Data
public class ProductProperties {
    
    /**
     * Threshold below which stock is considered low (triggers alerts)
     */
    @Min(1)
    @Max(100)
    private int lowStockThreshold = 10;
    
    /**
     * Standard batch processing size
     */
    @Min(10)
    @Max(1000)
    private int batchSize = 100;
    
    /**
     * Maximum allowed batch size for operations
     */
    @Min(10)
    @Max(1000)
    private int maxBatchSize = 100;
    
    /**
     * Maximum attempts to generate unique friendly URL
     */
    @Min(10)
    @Max(10000)
    private int maxFriendlyUrlAttempts = 1000;
    
    /**
     * Cache TTL for product statistics
     */
    @NotNull
    private Duration statisticsCacheTtl = Duration.ofMinutes(15);
    
    /**
     * Search configuration settings
     */
    @NotNull
    private SearchConfig search = new SearchConfig();
    
    /**
     * Nested configuration for search operations
     */
    @Data
    public static class SearchConfig {
        
        @Min(1)
        @Max(100)
        private int defaultPageSize = 20;
        
        @Min(1)
        @Max(1000)
        private int maxPageSize = 100;
        
        @Min(100)
        private int maxSearchResultsTotal = 10000;
        
        @Min(1)
        @Max(10000)
        private int maxPageNumber = 1000;
    }
}
