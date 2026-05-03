package com.eshop.app.config;

import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Pageable;
import org.springframework.util.DigestUtils;

import java.util.regex.Pattern;

/**
 * Cache key generation configuration.
 * Provides safe, deterministic key generators for caching operations.
 *
 * @author E-Shop Team
 * @since 2.0.0
 */
@Configuration
public class CacheKeyConfiguration {
    
    private static final Pattern SAFE_KEY_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]+$");
    private static final int MAX_KEY_LENGTH = 100;
    
    /**
     * Key generator for pageable queries with consistent sort handling.
     */
    @Bean
    public KeyGenerator pageableKeyGenerator() {
        return (target, method, params) -> {
            StringBuilder keyBuilder = new StringBuilder();
            keyBuilder.append(method.getName());
            
            for (Object param : params) {
                if (param instanceof Pageable pageable) {
                    keyBuilder.append("_p").append(pageable.getPageNumber());
                    keyBuilder.append("_s").append(pageable.getPageSize());
                    
                    if (pageable.getSort().isSorted()) {
                        pageable.getSort().forEach(order -> 
                            keyBuilder.append("_")
                                .append(order.getProperty())
                                .append(order.isAscending() ? "A" : "D")
                        );
                    }
                } else if (param != null) {
                    keyBuilder.append("_").append(sanitizeKeyPart(param));
                }
            }
            
            return truncateOrHash(keyBuilder.toString());
        };
    }
    
    /**
     * Safe key generator that sanitizes user input in keys.
     */
    @Bean
    public KeyGenerator safeCacheKeyGenerator() {
        return (target, method, params) -> {
            StringBuilder keyBuilder = new StringBuilder();
            keyBuilder.append(target.getClass().getSimpleName());
            keyBuilder.append(".");
            keyBuilder.append(method.getName());
            
            for (Object param : params) {
                keyBuilder.append(":");
                keyBuilder.append(sanitizeKeyPart(param));
            }
            
            return truncateOrHash(keyBuilder.toString());
        };
    }
    
    /**
     * Key generator for product-specific caching.
     */
    @Bean
    public KeyGenerator productKeyGenerator() {
        return (target, method, params) -> {
            if (params.length > 0) {
                Object firstParam = params[0];
                return "product:" + sanitizeKeyPart(firstParam);
            }
            return "product:default";
        };
    }
    
    /**
     * Key generator for search operations.
     */
    @Bean
    public KeyGenerator searchKeyGenerator() {
        return (target, method, params) -> {
            StringBuilder sb = new StringBuilder("search:");
            for (Object param : params) {
                if (param != null) {
                    if (param instanceof Pageable pageable) {
                        sb.append("p").append(pageable.getPageNumber())
                          .append("s").append(pageable.getPageSize());
                    } else {
                        sb.append(sanitizeKeyPart(param)).append(":");
                    }
                }
            }
            return truncateOrHash(sb.toString());
        };
    }
    
    /**
     * Sanitizes a parameter for safe cache key usage.
     */
    private String sanitizeKeyPart(Object param) {
        if (param == null) {
            return "null";
        }
        
        String value = param.toString();
        
        // If safe pattern matches, use directly
        if (SAFE_KEY_PATTERN.matcher(value).matches() && value.length() <= 50) {
            return value;
        }
        
        // Hash unsafe or long values
        return DigestUtils.md5DigestAsHex(value.getBytes());
    }
    
    /**
     * Truncates long keys or converts to hash.
     */
    private String truncateOrHash(String key) {
        if (key.length() > MAX_KEY_LENGTH) {
            return DigestUtils.md5DigestAsHex(key.getBytes());
        }
        return key;
    }
}
