package com.eshop.app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ConfigurationProperties(prefix = "app.cache")
@Validated
public class CacheProperties {

    /**
     * List of cache names to create
     */
    private List<String> cacheNames = List.of(
        "products", "categories", "brands", "users", "shops",
        "productsList", "categoriesList", "brandsList", "shopsList",
        // Dashboard & analytics caches
        "adminDashboard", "adminStatistics", "adminAnalytics",
        "sellerDashboard", "sellerStatistics", "sellerAnalytics"
    );

    /** Default spec applied when a cache has no specific configuration */
    private CacheSpec defaultSpec = new CacheSpec();

    /** Per-cache specifications (key = cache name) */
    private Map<String, CacheSpec> specs = new HashMap<>() {{
        put("adminDashboard", new CacheSpec() {{ setExpireAfterWrite(java.time.Duration.ofMinutes(5)); }});
        put("adminStatistics", new CacheSpec() {{ setExpireAfterWrite(java.time.Duration.ofMinutes(5)); }});
        put("adminAnalytics", new CacheSpec() {{ setExpireAfterWrite(java.time.Duration.ofMinutes(10)); }});
        put("sellerDashboard", new CacheSpec() {{ setExpireAfterWrite(java.time.Duration.ofMinutes(5)); }});
        put("sellerStatistics", new CacheSpec() {{ setExpireAfterWrite(java.time.Duration.ofMinutes(5)); }});
        put("sellerAnalytics", new CacheSpec() {{ setExpireAfterWrite(java.time.Duration.ofMinutes(10)); }});
    }};

    public List<String> getCacheNames() {
        return cacheNames;
    }

    public void setCacheNames(List<String> cacheNames) {
        this.cacheNames = cacheNames;
    }

    public CacheSpec getDefaultSpec() {
        return defaultSpec;
    }

    public void setDefaultSpec(CacheSpec defaultSpec) {
        this.defaultSpec = defaultSpec;
    }

    public Map<String, CacheSpec> getSpecs() {
        return specs;
    }

    public void setSpecs(Map<String, CacheSpec> specs) {
        this.specs = specs;
    }

    /**
     * Get specification for a specific cache, falling back to default
     */
    public CacheSpec getSpecForCache(String cacheName) {
        return specs.getOrDefault(cacheName, defaultSpec);
    }

    public static class CacheSpec {

        @Min(0)
        private int initialCapacity = 100;

        @Min(1)
        private long maximumSize = 10_000L;

        @DurationUnit(ChronoUnit.MINUTES)
        private Duration expireAfterAccess = Duration.ofMinutes(10);

        @DurationUnit(ChronoUnit.MINUTES)
        private Duration expireAfterWrite = Duration.ofMinutes(30);

        /** Enable async refresh before expiration */
        private boolean enableRefresh = false;

        @DurationUnit(ChronoUnit.MINUTES)
        private Duration refreshAfterWrite = Duration.ofMinutes(5);

        public int getInitialCapacity() {
            return initialCapacity;
        }

        public void setInitialCapacity(int initialCapacity) {
            this.initialCapacity = initialCapacity;
        }

        public long getMaximumSize() {
            return maximumSize;
        }

        public void setMaximumSize(long maximumSize) {
            this.maximumSize = maximumSize;
        }

        public Duration getExpireAfterAccess() {
            return expireAfterAccess;
        }

        public void setExpireAfterAccess(Duration expireAfterAccess) {
            this.expireAfterAccess = expireAfterAccess;
        }

        public Duration getExpireAfterWrite() {
            return expireAfterWrite;
        }

        public void setExpireAfterWrite(Duration expireAfterWrite) {
            this.expireAfterWrite = expireAfterWrite;
        }

        public boolean isEnableRefresh() {
            return enableRefresh;
        }

        public void setEnableRefresh(boolean enableRefresh) {
            this.enableRefresh = enableRefresh;
        }

        public Duration getRefreshAfterWrite() {
            return refreshAfterWrite;
        }

        public void setRefreshAfterWrite(Duration refreshAfterWrite) {
            this.refreshAfterWrite = refreshAfterWrite;
        }

        @Override
        public String toString() {
            return String.format(
                "CacheSpec{initialCapacity=%d, maxSize=%d, expireAccess=%s, expireWrite=%s}",
                initialCapacity, maximumSize, expireAfterAccess, expireAfterWrite
            );
        }
    }
}
