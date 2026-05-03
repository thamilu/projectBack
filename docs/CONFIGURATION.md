# Configuration Guide - eshop_back

**Last Updated:** 2026-01-14  
**Version:** 3.0

---

## Overview

This document describes the active configuration classes in the eshop_back project after consolidation (CRITICAL-003 fix). Duplicate and redundant configurations have been removed to eliminate confusion and potential bean conflicts.

---

## Active Configurations

### 1. Cache Configuration

**File:** [`CacheConfig.java`](file:///g:/Project/eshop_back/src/main/java/com/eshop/app/config/CacheConfig.java)

**Purpose:** Unified multi-level caching strategy with Caffeine (L1) and Redis (L2) support.

**Architecture:**
```
Request → L1 (Caffeine) → HIT? → Return
              ↓ MISS
          L2 (Redis) → HIT? → Populate L1 → Return
              ↓ MISS
          Database → Populate L1 & L2 → Return
```

**Cache Managers:**
- **Primary:** `CompositeCacheManager` - Combines Caffeine + Redis
- **Fallback:** Caffeine-only when Redis unavailable

**Cache Names & TTLs:**
| Cache Name | Caffeine TTL | Redis TTL | Use Case |
|------------|--------------|-----------|----------|
| `products` | 5 min | 30 min | Product details |
| `productSearch` | 5 min | 15 min | Search results |
| `categories` | 30 min | 60 min | Category listings |
| `statistics` | 2 min | 5 min | Real-time stats |
| `sessions` | N/A | 24 hours | User sessions |

**Configuration Properties:**
```properties
# Enable/disable Redis (falls back to Caffeine if disabled)
cache.redis.enabled=true

# Default TTLs
cache.redis.default-ttl=60      # minutes
cache.caffeine.default-ttl=10   # minutes
cache.caffeine.max-size=10000   # entries
```

**Status:** ✅ ACTIVE (all profiles)

---

### 2. Async Configuration

**File:** [`AsyncConfiguration.java`](file:///g:/Project/eshop_back/src/main/java/com/eshop/app/config/AsyncConfiguration.java)

**Purpose:** Provides multiple task executors optimized for different workload types.

**Available Executors:**

#### 1. `eshopVirtualThreadExecutor` (Default)
- **Type:** Virtual threads (Java 21)
- **Use for:** I/O-bound operations (database queries, API calls, file I/O)
- **Benefits:** Extremely lightweight, millions can be created

**Usage:**
```java
@Async
public CompletableFuture<String> fetchData() {
    // Uses virtual thread executor by default
}
```

#### 2. `cpuBoundExecutor`
- **Type:** Platform thread pool
- **Pool Size:** Based on CPU cores (cores to cores*2)
- **Use for:** CPU-intensive operations (image processing, report generation, encryption)

**Usage:**
```java
@Async("cpuBoundExecutor")
public CompletableFuture<Report> generateReport() {
    // Uses CPU-bound thread pool
}
```

#### 3. `dashboardExecutor`
- **Type:** Platform thread pool
- **Pool Size:** 4 core threads, 8 max threads
- **Use for:** Dashboard data aggregation and analytics

**Usage:**
```java
@Async("dashboardExecutor")
public CompletableFuture<DashboardData> loadDashboard() {
    // Uses dedicated dashboard executor
}
```

**Executor Selection Guide:**
- Database queries, API calls, file I/O → Use **default** (virtual threads)
- Image processing, report generation, data analysis → Use **cpuBoundExecutor**
- Dashboard data aggregation → Use **dashboardExecutor**

**Status:** ✅ ACTIVE (all profiles)

---

## Removed Configurations (As of 2026-01-14)

The following redundant configuration classes have been **removed** to eliminate duplication:

### ❌ CacheConfiguration.java
- **Reason:** Simple fallback entirely covered by `CacheConfig.java`
- **Replacement:** Use `CacheConfig.java` (already active)

### ❌ EnhancedCacheConfig.java
- **Reason:** Caffeine-only config, redundant with `CacheConfig.java`
- **Replacement:** Use `CacheConfig.java` (already active)

### ❌ AsyncConfig.java
- **Reason:** Only provided `dashboardExecutor`, less complete than `AsyncConfiguration.java`
- **Replacement:** `dashboardExecutor` migrated to `AsyncConfiguration.java`

---

## Profile-Specific Configurations

### Development Profile (`dev`)
- **Cache:** Caffeine + Redis (if available, otherwise Caffeine-only)
- **Async:** All executors enabled
- **Logging:** DEBUG level for cache hits/misses

### Production Profile (`prod`)
- **Cache:** Caffeine + Redis (required)
- **Async:** All executors enabled
- **Logging:** INFO level

### Test Profile (`test`)
- **Cache:** Caffeine-only (no Redis dependency)
- **Async:** All executors enabled

---

## Bean Names Reference

When injecting beans via `@Qualifier`, use these exact names:

**Cache Managers:**
- `cacheManager` - Primary composite cache manager
- `caffeineCacheManager` - L1 Caffeine cache manager
- `redisCacheManager` - L2 Redis cache manager (if enabled)

**Executors:**
- Default (no qualifier needed) - Virtual thread executor
- `"cpuBoundExecutor"` - CPU-bound tasks
- `"dashboardExecutor"` - Dashboard operations

**Example:**
```java
@Autowired
@Qualifier("cpuBoundExecutor")
private TaskExecutor cpuExecutor;
```

---

## Migration Notes

### If you were using removed configurations:

**1. CacheConfiguration.CacheNames**
```java
// OLD (removed)
import com.eshop.app.config.CacheConfiguration.CacheNames;

// NEW (use instead)
import com.eshop.app.config.CacheConfig;
// Constants: CacheConfig.PRODUCTS_CACHE, CacheConfig.CATEGORIES_CACHE, etc.
```

**2. enhancedCacheManager bean**
```java
// OLD (removed)
@Autowired
@Qualifier("enhancedCacheManager")
private CacheManager cacheManager;

// NEW (use default)
@Autowired
private CacheManager cacheManager;  // Gets composite cache manager
```

**3. dashboardExecutor (no change needed)**
```java
// This still works exactly the same
@Async("dashboardExecutor")
public CompletableFuture<Data> loadData() { ... }
```

---

## Troubleshooting

### Cache not working?
1. Check Redis is running: `redis-cli ping` should return `PONG`
2. Check logs for "Initialized Composite CacheManager" message
3. If Redis unavailable, app falls back to Caffeine-only (check logs for warning)

### Async methods not executing asynchronously?
1. Ensure `@EnableAsync` is present (already in `AsyncConfiguration.java`)
2. Don't call `@Async` methods from same class (Spring AOP limitation)
3. Methods must be `public` and return `void` or `Future/CompletableFuture`

### Bean definition conflicts?
- Should not occur after consolidation
- If you see errors, check for custom configurations overriding base configs

---

## Performance Tips

### Cache Optimization
- Use appropriate cache for data volatility
- Monitor cache hit ratios via actuator: `/actuator/caches`
- Adjust TTLs in `application.properties` if needed

### Async Optimization
- Use virtual threads (default) for most async operations
- Reserve `cpuBoundExecutor` for truly CPU-intensive work
- Monitor executor metrics via actuator: `/actuator/metrics`

---

## References

- [Spring Boot Caching Guide](https://docs.spring.io/spring-boot/docs/current/reference/html/io.html#io.caching)
- [Spring Async Documentation](https://docs.spring.io/spring-framework/reference/integration/scheduling.html#scheduling-annotation-support-async)
- [Caffeine Cache](https://github.com/ben-manes/caffeine)
- [Redis](https://redis.io/docs/)
- [Java Virtual Threads (JEP 444)](https://openjdk.org/jeps/444)

---

**For questions or issues, contact the E-Shop Team.**
