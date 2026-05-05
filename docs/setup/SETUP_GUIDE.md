

# --- File: CONFIGURATION.md ---

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


# --- File: DEVELOPMENT_SETUP.md ---

# ≡ƒÜÇ E-Shop Development Setup Guide

This document contains all the information needed to run, manage, and access the development infrastructure for the E-Shop backend.

## ≡ƒôº Quick Start Commands

| Action | Command |
| :--- | :--- |
| **Start Infrastructure** | `docker-compose -f docker-compose-dev.yml up -d` |
| **Stop Infrastructure** | `docker-compose -f docker-compose-dev.yml down` |
| **Reset Databases** | `docker-compose -f docker-compose-dev.yml down -v; docker-compose -f docker-compose-dev.yml up -d` |
| **Run Backend** | `./gradlew.bat clean bootRun --args="--spring.profiles.active=dev"` |

---

## Γ£à Service Access Points

### 1. PostgreSQL Database
Available on port `5432` with two distinct databases:
*   **Application DB**: `eshop_db` (Where products, orders, and users are stored)
*   **Keycloak DB**: `eshop_keycloak` (Internal auth data)
*   **Username**: `postgres`
*   **Password**: `thamilu*884*`

### 2. How to Access the Database (Step-by-Step)

#### Method A: Using pgAdmin (Recommended)
1.  **Open Browser**: Go to [http://localhost:5050](http://localhost:5050).
2.  **Login**: User: `admin@eshop.com`, Pass: `admin`.
3.  **Register Server**:
    *   Right-click **Servers** > **Register** > **Server...**
    *   **General**: Name it `E-Shop Local`.
    *   **Connection**:
        *   **Host name/address**: `postgres` (if using Docker network) or `localhost` (from your PC).
        *   **Port**: `5432`
        *   **Username**: `postgres`
        *   **Password**: `thamilu*884*`
    *   Click **Save**.

#### Method B: Using External Tools (DBeaver, IntelliJ, etc.)
Use these settings for any external database manager:
*   **Host**: `localhost`
*   **Port**: `5432`
*   **User**: `postgres`
*   **Pass**: `thamilu*884*`
*   **Database Names**: `eshop_db` (App) or `eshop_keycloak` (Auth).

### 3. Keycloak (Authentication)
*   **Admin Console**: [http://localhost:8080](http://localhost:8080)
*   **Admin User**: `admin`
*   **Admin Password**: `Admin@@Secret123`
*   **Realms**:
    *   `eshop`: Marketplace users (Sellers, Customers)
    *   `eshop-admin`: Administration (Approvals)

### 4. MailHog (Email Testing)
Captures every email sent by the system (e.g., OTPs, order confirmations).
*   **Web UI**: [http://localhost:8025](http://localhost:8025)

### 5. Redis Commander (Cache Management)
*   **Web UI**: [http://localhost:8081](http://localhost:8081)

---

## ≡ƒôƒ Environment Variables (.env)
Your `.env` file is the master configuration. Key variables include:
*   `KEYCLOAK_AUTH_SERVER`: Base URL for authentication.
*   `DB_URL`: JDBC connection string for the backend.
*   `JWT_SECRET`: Randomly generated 512-bit key for session security.

> [!NOTE]
> Never commit your `.env` file to Git. Use `.env.example` as a template for new environments.


# --- File: ENVIRONMENT_MANAGEMENT.md ---

# Γ£¬ Environment Variable Management

This document explains how the E-Shop backend manages configuration and secrets using a dynamic, zero-hardcoding approach.

## Γ£ì The Architecture

We use a **"Gradle-First"** loading strategy. This ensures that all environment variables are available to Spring Boot before it starts validating the configuration, preventing common errors like `NumberFormatException`.

### 1. The .env File
The [**.env**](file:///g:/Project/eshop_back/.env) file is the **Single Source of Truth**. 
- It contains all database passwords, JWT secrets, and realm configurations.
- **Security**: This file is ignored by Git and should never be committed.

### 2. Gradle Integration
The [**build.gradle**](file:///g:/Project/eshop_back/build.gradle) file contains a custom configuration for the `bootRun` task:
```groovy
tasks.named('bootRun').configure {
    if (file(".env").exists()) {
        file(".env").readLines().each { line ->
            // Parses key=value pairs and injects them into the JVM
        }
    }
}
```
This script reads your `.env` file and "injects" the values directly into the running application.

### 3. Clean Property Files
Because Gradle loads the variables early, our [**application.properties**](file:///g:/Project/eshop_back/src/main/resources/application.properties) files remain clean:
- **No hardcoded passwords**: Everything uses `${VAR_NAME}`.
- **No temporary fallbacks**: No more `:86400000` mixed into the code.

---

## Γî║ How to Add New Variables

1.  **Update .env**: Add your new variable (e.g., `NEW_SETTING=example`).
2.  **Update Properties**: Reference it in the appropriate `application.properties` file using `${NEW_SETTING}`.
3.  **Use in Java**: Access it via `@Value("${NEW_SETTING}")` or the `AppProperties` class.

## ΓÜá∩╕Å Troubleshooting

- **"Property Not Found"**: Ensure the variable name in your `.env` exactly matches the placeholder in your `.properties` file (Case-Sensitive).
- **Gradle Message**: When you start the app, look for the message: `Γ£à Loaded environment variables from .env` to confirm the loading succeeded.


# --- File: RUNNING.md ---

Run & Deploy
===========

Overview
--------
This document explains how to run the application locally for development, testing, and production-like runs on Windows (PowerShell). It also includes common troubleshooting steps (e.g. missing Spring Security JOSE dependency).

Profiles and properties
-----------------------
Spring Boot picks up `application-<profile>.properties` automatically when you set `spring.profiles.active`.
This project provides:
- `application-dev.properties`
- `application-test.properties`
- `application-prod.properties`

Quick run (bootRun)
--------------------
Use `bootRun` for quick local runs (development):

PowerShell examples

Run with `dev` profile:
```powershell
./gradlew.bat bootRun --args='--spring.profiles.active=dev'
```

Run with `prod` profile (development runner only — prefer jar in real prod):
```powershell
./gradlew.bat bootRun --args='--spring.profiles.active=prod'
```

Run with `test` profile:
```powershell
./gradlew.bat bootRun --args='--spring.profiles.active=test'
```

Alternative ways to set the active profile
------------------------------------------
Set environment variable (PowerShell):
```powershell
$env:SPRING_PROFILES_ACTIVE='prod'
./gradlew.bat bootRun
```

Use JVM system property (Gradle/Different invocation):
```powershell
./gradlew.bat bootRun -Dspring.profiles.active=prod
```

Recommended production flow (jar)
---------------------------------
1. Build an executable jar (preferred for prod):
```powershell
./gradlew.bat clean bootJar
```
2. Run the produced jar from `build/libs`:
```powershell
java -jar build\libs\<your-app>-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```
Replace `<your-app>-0.0.1-SNAPSHOT.jar` with the actual artifact name you find in `build/libs`.

Running tests with a profile
----------------------------
Pass a profile via system property or env var:
```powershell
./gradlew.bat test -Dspring.profiles.active=test
# or
$env:SPRING_PROFILES_ACTIVE='test'
./gradlew.bat test
```

Troubleshooting: missing OAuth2 JOSE classes
-------------------------------------------
Problem: compile errors like
```
cannot find symbol: class OAuth2TokenValidator
```
Cause: `spring-security-oauth2-jose` is not on the classpath. That module provides JWT validation APIs used by custom validators (e.g. `JwtAudienceValidatorConfig`).

Recommended fix (Gradle)
Add the resource-server starter which pulls in the JOSE module transitively:

In `build.gradle` (dependencies block):
```gradle
implementation 'org.springframework.boot:spring-boot-starter-security'
implementation 'org.springframework.boot:spring-boot-starter-oauth2-resource-server'
```

Alternative (add only JOSE):
```gradle
implementation 'org.springframework.security:spring-security-oauth2-jose'
```

After adding dependency run:
```powershell
./gradlew.bat clean build
```

If the build still fails:
- Check for dependency exclusions in `build.gradle` or `dependencyManagement` overrides.
- Ensure Spring Security versions are compatible with Spring Boot 4's managed versions.

Notes & Recommendations
-----------------------
- `bootRun` is convenient for local development but not for production. Use `bootJar` + `java -jar` for production-like runs.
- For CI, run `./gradlew.bat clean build` and run integration tests in a container or test environment with `--spring.profiles.active=test`.
- To see verbose Spring Boot startup logs, add `--debug` or set `logging.level.root=DEBUG` in appropriate `application-*.properties`.

Want me to:
- build the `prod` jar now and run it, or
- run `./gradlew.bat clean build` to verify fixes on this machine?


# --- File: Keycloak-SpringBoot-Docker-Setup.md ---

# Keycloak + Spring Boot Integration Guide (with Docker)

## 1. Run Keycloak with Docker

Create a file named `keycloak-docker-compose.yml` with the following content:

```yaml
version: '3.8'
services:
  keycloak:
    image: quay.io/keycloak/keycloak:24.0.0
    command: start-dev --http-port=8080
    environment:
      KEYCLOAK_ADMIN: admin
      KEYCLOAK_ADMIN_PASSWORD: admin
    ports:
      - "8080:8080"
```

Start Keycloak:
```sh
docker compose -f keycloak-docker-compose.yml up
```

---

## 2. Create the 'eshop' Realm in Keycloak

1. Open [http://localhost:8080](http://localhost:8080) and log in as `admin`/`admin`.
2. In the admin console, click the realm dropdown (top left), then click **Create realm**.
3. Enter `eshop` as the realm name and click **Create**.

---

## 3. Create a Client for Your App

1. In the 'eshop' realm, go to **Clients** > **Create client**.
2. Set `Client ID` to `eshop-client`.
3. Set **Client Protocol** to `openid-connect`.
4. Set **Access Type** to `public` (or `confidential` if you want to use a client secret).
5. Enable **Direct Access Grants** (for password grant testing).
6. Save.

---

## 4. Create a Test User

1. In the 'eshop' realm, go to **Users** > **Add user**.
2. Fill in username and other details, then save.
3. Go to **Credentials** tab, set a password, and turn off "Temporary".
4. Assign roles if your API requires them.

---

## 5. Configure Spring Boot

In your `application.properties`:
```
spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8080/realms/eshop
```

No need to set the public key manually—Spring Boot will fetch it from Keycloak.

---

## 6. Get a Token and Test with Postman

### Get Token
- POST to: `http://localhost:8080/realms/eshop/protocol/openid-connect/token`
- Body (x-www-form-urlencoded):
    - grant_type: password
    - client_id: eshop-client
    - username: <your-user>
    - password: <your-password>
    - client_secret: <if confidential client>

Copy the `access_token` from the response.

### Call Your API
- Set `Authorization: Bearer <access_token>` header in Postman.
- POST to your API endpoint (e.g., `http://localhost:8082/api/v1/products`).
- Provide the required JSON body for product creation.

---

## 7. Troubleshooting
- If you get `JWT invalid` or `Another algorithm expected`, make sure:
    - You use a Keycloak-issued token (not a hardcoded or third-party token).
    - The client uses RS256 (default in Keycloak).
    - The token is for the correct realm (`eshop`).
- If you get 401/403, check user roles and token validity.

---

## 8. Useful Endpoints
- OpenID config: [http://localhost:8080/realms/eshop/.well-known/openid-configuration](http://localhost:8080/realms/eshop/.well-known/openid-configuration)
- JWKS (public keys): [http://localhost:8080/realms/eshop/protocol/openid-connect/certs](http://localhost:8080/realms/eshop/protocol/openid-connect/certs)

---

**This document summarizes all steps to set up Keycloak with Docker, configure a realm, client, user, and test Spring Boot API authentication.**



# --- File: deployment-checklist.md ---

# 🚀 Deployment Checklist - Enterprise Refactoring

## Pre-Deployment Validation

### 1. Database Migrations
```bash
# Check pending migrations
./gradlew flywayInfo

# Expected new migrations:
# - V2026_01_01_001__create_shedlock_table.sql
# - V2026_01_01_002__enterprise_performance_indexes.sql

# Run migrations (dry-run first in dev)
./gradlew flywayMigrate
```

### 2. Build Verification
```bash
# Clean build with tests
./gradlew clean build

# Expected: BUILD SUCCESSFUL
# All tests should pass
```

### 3. Configuration Review

#### application.properties - New Properties
```properties
# Rate Limiting (Resilience4j already configured)
✓ 7 rate limiter instances defined

# File Upload Security
✓ app.upload.max-file-size=5242880
✓ app.upload.allowed-mime-types=image/jpeg,image/png,image/webp
✓ app.upload.max-image-width=4096
✓ app.upload.max-image-height=4096

# ShedLock
✓ shedlock table will be created by migration

# Correlation IDs
✓ CorrelationIdFilter automatically registered

# Cache Configuration
✓ Two-tier caching (Caffeine + Redis) already configured
```

---

## Deployment Steps

### Step 1: Database Backup
```bash
# Backup production database
pg_dump -h $DB_HOST -U $DB_USER -d eshop_db > backup_$(date +%Y%m%d_%H%M%S).sql
```

### Step 2: Run Migrations
```bash
# Apply migrations
./gradlew flywayMigrate -Dflyway.url=$PROD_DB_URL -Dflyway.user=$DB_USER -Dflyway.password=$DB_PASSWORD

# Verify migrations
./gradlew flywayInfo
```

### Step 3: Deploy Application
```bash
# Build production JAR
./gradlew clean bootJar

# Deploy to server
scp build/libs/eshop-0.0.1-SNAPSHOT.jar user@server:/opt/eshop/

# Restart application
ssh user@server 'systemctl restart eshop'
```

### Step 4: Health Checks
```bash
# Wait for startup
sleep 30

# Check health
curl http://server:8082/actuator/health

# Expected response:
# {"status":"UP"}

# Check rate limiter health
curl http://server:8082/actuator/health/rateLimiter

# Check cache health
curl http://server:8082/actuator/health/redis
```

---

## Post-Deployment Validation

### 1. API Functionality
```bash
# Test rate limiting
for i in {1..10}; do
    curl -H "X-Correlation-Id: test-$i" http://server:8082/api/v1/products
done

# Expected: First 5 succeed, then 429 Too Many Requests
```

### 2. Correlation IDs
```bash
# Make request
curl -v http://server:8082/api/v1/products

# Verify response headers:
# X-Correlation-Id: <uuid>
# X-Request-Id: <uuid>
```

### 3. File Upload Security
```bash
# Test with valid image
curl -F "file=@test.jpg" http://server:8082/api/v1/products/1/images

# Expected: 201 Created

# Test with oversized file
curl -F "file=@large.jpg" http://server:8082/api/v1/products/1/images

# Expected: 400 Bad Request with validation error
```

### 4. Distributed Scheduling
```bash
# Check ShedLock table
psql -h $DB_HOST -U $DB_USER -d eshop_db -c "SELECT * FROM shedlock;"

# Should show locked tasks when scheduled jobs run
```

### 5. Exception Handling
```bash
# Trigger validation error
curl -X POST http://server:8082/api/v1/products \
  -H "Content-Type: application/json" \
  -d '{"name": ""}'

# Expected: 400 with detailed field errors and correlationId
```

### 6. Metrics & Monitoring
```bash
# Prometheus metrics
curl http://server:8082/actuator/prometheus | grep resilience4j

# Rate limiter metrics
curl http://server:8082/actuator/metrics/resilience4j.ratelimiter.available.permissions

# Cache metrics
curl http://server:8082/actuator/metrics/cache.gets
```

---

## Monitoring Setup

### 1. Grafana Dashboards

#### Rate Limiter Dashboard
```
Panel 1: Rate Limit Violations (by limiter name)
Query: sum by(name)(rate(resilience4j_ratelimiter_available_permissions[5m]))

Panel 2: Rate Limit Hit Rate
Query: rate(security_csp_violations_total[5m])
```

#### Exception Dashboard
```
Panel 1: Exceptions by Type
Query: sum by(exception)(rate(exceptions_total[5m]))

Panel 2: Response Codes
Query: sum by(status)(rate(http_server_requests_seconds_count[5m]))
```

### 2. Alert Rules

#### Critical Alerts
```yaml
# Rate Limit Abuse
- alert: RateLimitAbuse
  expr: rate(rate_limit_exceeded_total[5m]) > 100
  for: 5m
  labels:
    severity: warning
  annotations:
    summary: "High rate limit violations"

# ShedLock Failures
- alert: ScheduledTaskLockFailure
  expr: shedlock_lock_failures_total > 10
  for: 10m
  labels:
    severity: critical
  annotations:
    summary: "Scheduled tasks failing to acquire locks"

# File Upload Abuse
- alert: FileUploadAbuse
  expr: rate(file_upload_validation_failures_total[5m]) > 50
  for: 5m
  labels:
    severity: warning
  annotations:
    summary: "High file upload validation failures"
```

---

## Rollback Plan

### If Issues Occur

#### 1. Application Rollback
```bash
# Stop new version
ssh user@server 'systemctl stop eshop'

# Deploy previous version
scp backup/eshop-previous.jar user@server:/opt/eshop/eshop-0.0.1-SNAPSHOT.jar

# Start application
ssh user@server 'systemctl start eshop'
```

#### 2. Database Rollback
```bash
# ShedLock table (if needed)
psql -h $DB_HOST -U $DB_USER -d eshop_db -c "DROP TABLE IF EXISTS shedlock;"

# Performance indexes (if causing issues)
# Note: Indexes can be dropped without affecting data
psql -h $DB_HOST -U $DB_USER -d eshop_db -c "
DROP INDEX CONCURRENTLY IF EXISTS idx_products_fulltext_search;
DROP INDEX CONCURRENTLY IF EXISTS idx_products_active_category_price;
-- etc.
"

# Restore from backup (last resort)
psql -h $DB_HOST -U $DB_USER -d eshop_db < backup_YYYYMMDD_HHMMSS.sql
```

---

## Performance Validation

### 1. Query Performance
```sql
-- Check index usage after deployment
SELECT 
    schemaname, 
    tablename, 
    indexname, 
    idx_scan as scans,
    idx_tup_read as tuples_read
FROM pg_stat_user_indexes
WHERE schemaname = 'public'
  AND indexname LIKE 'idx_%'
ORDER BY idx_scan DESC
LIMIT 20;

-- Expected: New indexes show increasing scan counts
```

### 2. Response Time Improvement
```bash
# Before/After comparison
# Average response time should decrease by 30-50%

# Check p95 latency
curl http://server:8082/actuator/metrics/http.server.requests | jq '.measurements[] | select(.statistic == "VALUE") | .value'

# Expected: < 200ms for most endpoints
```

### 3. Cache Hit Ratio
```bash
# Redis cache hits
curl http://server:8082/actuator/metrics/cache.gets | jq '.measurements[] | select(.statistic == "COUNT") | .value'

# Expected: 80%+ hit rate after warmup
```

---

## Security Validation

### 1. Penetration Testing
```bash
# Rate limit bypass attempts (should fail)
for i in {1..200}; do
    curl -H "X-Forwarded-For: 192.168.$i.1" http://server:8082/api/v1/products &
done
wait

# Expected: 429 responses after limit exceeded

# File upload attack attempts
curl -F "file=@malicious.exe" http://server:8082/api/v1/products/1/images
# Expected: 400 Bad Request (extension not allowed)

curl -F "file=@fake_image.txt" http://server:8082/api/v1/products/1/images
# Expected: 400 Bad Request (MIME type mismatch)
```

### 2. Correlation ID Injection
```bash
# Test correlation ID persistence
CORR_ID="test-$(date +%s)"
curl -H "X-Correlation-Id: $CORR_ID" http://server:8082/api/v1/products

# Check logs
grep "$CORR_ID" /var/log/eshop/application.log

# Expected: All log entries for this request have same correlation ID
```

---

## Stakeholder Communication

### Deployment Notification Template

```
Subject: E-Shop Enterprise Refactoring - Deployment Complete

Dear Team,

The enterprise refactoring has been successfully deployed to production.

Key Improvements:
✓ Rate Limiting: 7-tier protection against API abuse
✓ File Upload Security: Multi-layer validation
✓ Enhanced Error Handling: 25+ exception handlers
✓ Distributed Locking: Zero duplicate job execution
✓ Performance: 60-80% faster queries with new indexes
✓ Observability: Correlation IDs and distributed tracing

New Features:
- Rate limiting on all API endpoints
- Secure file upload validation
- Comprehensive error responses with correlation IDs
- Distributed scheduling with ShedLock

Documentation:
- Complete Summary: ENTERPRISE_REFACTORING_COMPLETE_2026.md
- Quick Reference: QUICK_REFERENCE.md
- API Docs: http://server:8082/swagger-ui.html

Monitoring:
- Grafana: http://grafana.company.com
- Prometheus: http://prometheus.company.com
- Zipkin: http://zipkin.company.com

Issues: Report to #eshop-support with correlation ID

Deployment Time: [TIMESTAMP]
Downtime: [DURATION]
Status: ✅ Successful
```

---

## Success Criteria

### Must Pass (Go/No-Go)
- ✅ All health checks passing
- ✅ Rate limiting functional
- ✅ File uploads validated correctly
- ✅ Correlation IDs in logs
- ✅ ShedLock preventing duplicate jobs
- ✅ Zero critical errors in first hour
- ✅ Response times within SLA

### Nice to Have
- ✅ Cache hit rate > 80%
- ✅ Query performance improvement > 50%
- ✅ Rate limit violations logged
- ✅ Distributed tracing working

---

## Support Contact

**On-Call Engineer:** [Name]
**Slack Channel:** #eshop-support
**PagerDuty:** [Link]

**Escalation Path:**
1. Check logs with correlation ID
2. Review Grafana dashboards
3. Check Zipkin traces
4. Contact on-call engineer

---

## Appendix: New Files

```
Created:
├── RateLimitConfiguration.java
├── RateLimitingAspect.java
├── RateLimited.java
├── RateLimitKeyType.java
├── ShedLockConfiguration.java
├── SecureFileUploadService.java
├── ApiError.java
├── V2026_01_01_001__create_shedlock_table.sql
├── V2026_01_01_002__enterprise_performance_indexes.sql
├── ENTERPRISE_REFACTORING_COMPLETE_2026.md
└── QUICK_REFERENCE.md

Modified:
├── GlobalExceptionHandler.java (25+ handlers)
├── RateLimitExceededException.java (added fields)
└── CspReportController.java (fixed deprecated API)
```

---

**Deployment Date:** 2026-01-01
**Status:** ✅ PRODUCTION READY


# --- File: dev-setup-hybrid.md ---

# 🚀 Local Development Setup

**Keycloak + Redis in Docker, Spring Boot Backend Locally**

---

## 🎯 Overview

This setup allows you to:
- ✅ Run **Keycloak** and **Redis** in Docker (infrastructure)
- ✅ Run **Spring Boot backend** locally (on your machine)
- ✅ Hot reload and debug backend easily
- ✅ No need to rebuild Docker images for code changes

---

## 📋 Prerequisites

- Docker Desktop installed and running
- Java 21 installed
- Gradle (wrapper included)

---

## 🚀 Quick Start (One Command)

For quick path navigation and starting the entire environment from a fresh terminal:

```powershell
# 1. Navigate to project root
cd /d G:\Project\eshop_back

# 2. Start infrastructure (Keycloak + Redis + Postgres)
docker compose -f docker-compose-dev.yml up -d

# 3. Run Spring Boot app
.\gradlew bootRun
```

---

## 🚀 Detailed Setup Options


### Option 1: Automated (Recommended)

**Windows PowerShell:**
```powershell
.\start-dev-infra.ps1
```

**Linux/Mac:**
```bash
chmod +x start-dev-infra.sh
./start-dev-infra.sh
```

### Option 2: Manual Steps

```bash
# 1. Start Keycloak + Redis in Docker
docker compose -f docker-compose-dev.yml up -d

# 2. Wait for services (30-60 seconds)
docker compose -f docker-compose-dev.yml ps

# 3. Run Spring Boot backend locally
.\gradlew.bat bootRun --args="--spring.profiles.active=dev"
```

---

## 📊 What's Running Where

| Service | Where | URL | Purpose |
|---------|-------|-----|---------|
| **Keycloak** | Docker | http://localhost:8080 | Authentication |
| **Redis** | Docker | localhost:6379 | Cache |
| **PostgreSQL** | Docker | localhost:5432 | Database |
| **Spring Boot** | Local | http://localhost:8082 | Your API |

---

## 🔧 Configuration

### Docker Services (docker-compose-dev.yml)

**Services included:**
- ✅ Redis 7 Alpine
- ✅ PostgreSQL 16 Alpine
- ✅ Keycloak 23.0

**Exposed Ports:**
- Redis: `6379`
- PostgreSQL: `5432`
- Keycloak: `8080`

### Spring Boot (application-dev.properties)

**Profile:** `dev`

**Connections:**
```properties
spring.data.redis.host=localhost      # Redis in Docker
spring.data.redis.port=6379

spring.datasource.url=jdbc:postgresql://localhost:5432/eshop_Dev

keycloak.auth-server-url=http://localhost:8080
```

---

## 🧪 Verify Everything Works

### 1. Check Docker Services
```powershell
docker compose -f docker-compose-dev.yml ps
```

**Expected:** All services `healthy`

### 2. Test Redis
```bash
docker exec -it eshop-redis-dev redis-cli ping
# Expected: PONG
```

### 3. Test Keycloak
Open browser: http://localhost:8080

**Login:** admin / admin

### 4. Run Spring Boot Backend
```powershell
.\gradlew.bat bootRun --args="--spring.profiles.active=dev"
```

**Expected output:**
```
Started EshopApplication in X.XXX seconds
Featured products cache warmed
```

### 5. Test Backend Health
```bash
curl http://localhost:8082/actuator/health
```

**Expected:**
```json
{
  "status": "UP",
  "components": {
    "redis": { "status": "UP" }
  }
}
```

---

## 🔍 Useful Commands

### Docker Management

```bash
# View all logs
docker compose -f docker-compose-dev.yml logs -f

# View specific service logs
docker compose -f docker-compose-dev.yml logs -f redis
docker compose -f docker-compose-dev.yml logs -f keycloak

# Restart services
docker compose -f docker-compose-dev.yml restart

# Stop all services
docker compose -f docker-compose-dev.yml down

# Stop and remove volumes (fresh start)
docker compose -f docker-compose-dev.yml down -v
```

### Backend Management

```bash
# Run with dev profile
.\gradlew.bat bootRun --args="--spring.profiles.active=dev"

# Build without tests
.\gradlew.bat clean build -x test

# Run tests
.\gradlew.bat test

# Clean build
.\gradlew.bat clean
```

### Redis Commands

```bash
# Enter Redis container
docker exec -it eshop-redis-dev redis-cli

# Check keys
keys *

# Monitor Redis activity
monitor

# Clear all cache
flushall
```

---

## 🐛 Troubleshooting

### Problem: "Connection refused" to Redis
**Solution:**
```bash
# Check if Redis is running
docker ps | grep redis

# If not running, start it
docker compose -f docker-compose-dev.yml up -d redis

# Check Redis health
docker inspect eshop-redis-dev | grep Health
```

### Problem: Backend can't connect to Keycloak
**Solution:**
1. Wait 60 seconds for Keycloak to fully start
2. Check: http://localhost:8080
3. Verify logs: `docker compose -f docker-compose-dev.yml logs keycloak`

### Problem: Port already in use
**Solutions:**

**Redis (6379):**
```bash
# Windows
netstat -ano | findstr :6379
taskkill /PID <PID> /F

# Linux/Mac
lsof -ti:6379 | xargs kill -9
```

**Keycloak (8080):**
```bash
# Windows
netstat -ano | findstr :8080
taskkill /PID <PID> /F

# Linux/Mac
lsof -ti:8080 | xargs kill -9
```

### Problem: Backend starts but dashboard returns 500
**Cause:** Redis not connected

**Solution:**
1. Check Redis health: `docker ps`
2. Verify application-dev.properties has `spring.data.redis.host=localhost`
3. Test Redis: `docker exec -it eshop-redis-dev redis-cli ping`

---

## 💡 Development Workflow

### Starting Your Day
```bash
# 1. Start infrastructure
.\start-dev-infra.ps1

# 2. Wait for services (check with docker ps)
docker compose -f docker-compose-dev.yml ps

# 3. Start backend from IDE or terminal
.\gradlew.bat bootRun --args="--spring.profiles.active=dev"
```

### Ending Your Day
```bash
# Stop Docker services (keeps data)
docker compose -f docker-compose-dev.yml down

# Or stop and remove volumes (fresh start next time)
docker compose -f docker-compose-dev.yml down -v
```

### Making Changes
- Backend code changes → Just save and restart (or use hot reload)
- No need to rebuild Docker images
- Database schema changes → Hibernate will auto-update (ddl-auto=update)

---

## 🎯 IDE Configuration

### IntelliJ IDEA

**Run Configuration:**
1. Edit Run Configuration
2. Set Main class: `com.eshop.app.EshopApplication`
3. Add VM options: `-Dspring.profiles.active=dev`
4. Apply and Run

### VS Code

**.vscode/launch.json:**
```json
{
  "type": "java",
  "name": "EShop Dev",
  "request": "launch",
  "mainClass": "com.eshop.app.EshopApplication",
  "args": "--spring.profiles.active=dev",
  "projectName": "eshop"
}
```

---

## 📈 Advantages of This Setup

✅ **Fast Development**
- No Docker image rebuilds
- Instant code changes
- Easy debugging

✅ **Isolated Infrastructure**
- Keycloak and Redis in containers
- Clean separation of concerns
- Easy to reset

✅ **Flexible**
- Can run backend in IDE
- Full debugging support
- Hot reload works

✅ **Realistic**
- Same services as production
- Tests real Redis behavior
- Keycloak integration

---

## 🔄 Switching to Full Docker

Need to run everything in Docker? Use:
```bash
docker compose -f docker-compose.yml up -d
```

This will run backend in Docker too (profile: docker).

---

## 📚 Related Documentation

- [docker-compose-dev.yml](docker-compose-dev.yml) - Development infrastructure
- [docker-compose.yml](docker-compose.yml) - Full Docker setup
- [application-dev.properties](src/main/resources/application-dev.properties) - Dev configuration
- [DOCKER_REDIS_FIX.md](DOCKER_REDIS_FIX.md) - Full Docker setup guide

---

## ✅ Quick Checklist

Before reporting issues, verify:

- [ ] Docker Desktop is running
- [ ] All containers are healthy: `docker ps`
- [ ] Redis responds: `docker exec -it eshop-redis-dev redis-cli ping`
- [ ] Keycloak is accessible: http://localhost:8080
- [ ] Backend uses `dev` profile: Check startup logs
- [ ] Ports 6379, 8080, 5432, 8082 are not in use

---

**Status:** ✅ Ready for development  
**Setup Time:** ~2 minutes  
**Best For:** Active development with frequent code changes

Run `.\start-dev-infra.ps1` and start coding! 🚀


# --- File: docker-redis-fix.md ---

# 🐳 Docker Setup Complete - Redis Fixed!

**Date:** January 1, 2026  
**Status:** ✅ Redis networking configured for Docker

---

## 🎯 What Was Fixed

### ❌ The Problem
```
Caused by: io.lettuce.core.RedisConnectionException:
Unable to connect to localhost:6379
Connection refused
```

**Root Cause:** Backend running in Docker was trying to connect to `localhost:6379`, but Redis is in a separate container. Inside Docker, `localhost` ≠ Redis container.

### ✅ The Solution
Use Docker service names for inter-container communication on the same Docker network.

---

## 📁 Files Created

### 1. **application-docker.properties** ✅
**Location:** `src/main/resources/application-docker.properties`

**Key Changes:**
```properties
# ⚠️ CRITICAL: Use service name 'redis', NOT 'localhost'
spring.data.redis.host=redis
spring.data.redis.port=6379

# PostgreSQL also uses service name
spring.datasource.url=jdbc:postgresql://postgres:5432/eshop_db

# Keycloak uses service name
spring.security.oauth2.resourceserver.jwt.issuer-uri=http://keycloak:8080/realms/eshop
```

### 2. **docker-compose.yml** ✅
**Location:** Project root

**Services Included:**
- ✅ **Redis** (redis:7-alpine) - Port 6379
- ✅ **PostgreSQL** (postgres:16-alpine) - Port 5432
- ✅ **Keycloak** (quay.io/keycloak/keycloak:23.0) - Port 8080
- ✅ **Backend** (Spring Boot) - Port 8082

**Network Configuration:**
```yaml
networks:
  eshop-net:
    driver: bridge
    name: eshop-network
```

All services on the **same network** = DNS resolution works!

### 3. **Updated .env.example** ✅
```bash
REDIS_HOST=redis              # Service name
POSTGRES_DB=eshop_db
DATABASE_USERNAME=postgres
KEYCLOAK_ADMIN=admin
```

### 4. **Docker Startup Scripts** ✅
- **docker-start.ps1** (Windows PowerShell)
- **docker-start.sh** (Linux/Mac Bash)

Automated:
- Build Spring Boot app
- Build Docker images
- Start all containers
- Health checks
- Status verification

---

## 🚀 How to Use

### Option 1: Quick Start (Automated)

**Windows:**
```powershell
.\docker-start.ps1
```

**Linux/Mac:**
```bash
chmod +x docker-start.sh
./docker-start.sh
```

### Option 2: Manual Steps

```bash
# 1. Create .env file
cp .env.example .env

# 2. Build Spring Boot application
.\gradlew.bat clean build -x test

# 3. Start Docker containers
docker compose up -d

# 4. Watch logs
docker compose logs -f backend
```

---

## 📊 Service Endpoints

| Service | URL | Container Name |
|---------|-----|----------------|
| **Backend API** | http://localhost:8082 | eshop-backend |
| **Swagger UI** | http://localhost:8082/swagger-ui.html | eshop-backend |
| **Keycloak Admin** | http://localhost:8080 | eshop-keycloak |
| **PostgreSQL** | localhost:5432 | eshop-postgres |
| **Redis** | localhost:6379 | eshop-redis |
| **Health Check** | http://localhost:8082/actuator/health | eshop-backend |

**Keycloak Credentials:**
- Username: `admin`
- Password: `admin`

---

## 🔍 Verify Redis Connection

### Method 1: Check Backend Health
```bash
curl http://localhost:8082/actuator/health
```

**Expected Output:**
```json
{
  "status": "UP",
  "components": {
    "redis": {
      "status": "UP"
    }
  }
}
```

### Method 2: Test from Inside Backend Container
```bash
# Enter backend container
docker exec -it eshop-backend sh

# Ping Redis (install redis-cli first)
apk add redis
redis-cli -h redis ping
# Should return: PONG
```

### Method 3: Check Docker Logs
```bash
# Backend logs
docker compose logs backend

# Should see:
# ✅ "Featured products cache warmed"
# ✅ No "RedisConnectionException"
```

---

## 🧪 Test the Seller Dashboard

```bash
# 1. Get JWT token from Keycloak
curl -X POST "http://localhost:8080/realms/eshop/protocol/openid-connect/token" \
  -d "client_id=eshop-client" \
  -d "client_secret=YOUR_SECRET" \
  -d "username=seller@test.com" \
  -d "password=password" \
  -d "grant_type=password" | jq -r '.access_token'

# 2. Save token
$TOKEN = "<paste_token_here>"

# 3. Test seller dashboard (should work now!)
curl -H "Authorization: Bearer $TOKEN" http://localhost:8082/api/v1/dashboard/seller
```

**Expected:** `200 OK` with dashboard data (no more 500 error!)

---

## 🔧 Useful Docker Commands

```bash
# View all logs
docker compose logs -f

# View backend logs only
docker compose logs -f backend

# View Redis logs
docker compose logs -f redis

# Check container status
docker compose ps

# Restart backend
docker compose restart backend

# Stop all containers
docker compose down

# Stop and remove volumes (fresh start)
docker compose down -v

# Rebuild and restart
docker compose up -d --build
```

---

## 🐛 Troubleshooting

### Problem: "Connection refused" still appears
**Solution:**
1. Verify `SPRING_PROFILES_ACTIVE=docker` is set
2. Check `application-docker.properties` has `spring.data.redis.host=redis`
3. Ensure all containers are on same network:
   ```bash
   docker network inspect eshop-network
   ```

### Problem: Backend won't start
**Solution:**
1. Check logs: `docker compose logs backend`
2. Ensure Redis and PostgreSQL are healthy:
   ```bash
   docker ps
   ```
3. Wait 60-90 seconds for all services to initialize

### Problem: Redis not healthy
**Solution:**
```bash
# Check Redis logs
docker compose logs redis

# Restart Redis
docker compose restart redis

# Test Redis manually
docker exec -it eshop-redis redis-cli ping
```

### Problem: Can't connect to Keycloak
**Solution:**
1. Wait for Keycloak startup (takes 60+ seconds)
2. Check: http://localhost:8080
3. Verify logs: `docker compose logs keycloak`

---

## 📈 What's Different

### Before (❌ Broken)
```properties
# application.properties
spring.data.redis.host=localhost  # ❌ Won't work in Docker
```

### After (✅ Working)
```properties
# application-docker.properties
spring.data.redis.host=redis      # ✅ Docker service name
```

**Why it works:**
- Docker's internal DNS resolves `redis` → `172.18.0.2` (container IP)
- All containers on `eshop-net` can communicate
- No need to hardcode IPs

---

## ✅ Success Checklist

- ✅ `application-docker.properties` created with `redis` service name
- ✅ `docker-compose.yml` includes Redis, PostgreSQL, Keycloak, Backend
- ✅ All services on same Docker network (`eshop-net`)
- ✅ `.env.example` updated with Docker defaults
- ✅ Startup scripts created (PowerShell + Bash)
- ✅ Health checks configured for all services
- ✅ Dependencies ordered (Redis/Postgres before Backend)

---

## 🎉 Result

**Before:**
```
GET /api/v1/dashboard/seller
❌ 500 Internal Server Error
Caused by: RedisConnectionException: Unable to connect to localhost:6379
```

**After:**
```
GET /api/v1/dashboard/seller
✅ 200 OK
{
  "statistics": { ... },
  "recentOrders": [ ... ]
}
```

**Redis cache is now working! 🚀**

---

## 📚 Next Steps (Optional)

1. **Configure Keycloak:**
   - Create `eshop` realm
   - Add realm roles mapper (see JWT_AUTHENTICATION_IMPLEMENTATION.md)
   - Create test users

2. **Production Hardening:**
   - Add Redis password
   - Configure SSL/TLS
   - Use secrets management
   - Enable Redis persistence

3. **Monitoring:**
   - Add Prometheus
   - Add Grafana dashboards
   - Configure alerts

---

**Status:** ✅ Redis connection issue FIXED!  
**Build:** ✅ Successful  
**Docker:** ✅ Ready to use  

Run `.\docker-start.ps1` and you're good to go! 🎯


# --- File: environment-variables-guide.md ---

# Environment Variables Configuration Guide

This guide explains how to configure environment variables for the eShop backend application across different deployment environments.

## 📋 Overview

Spring Boot automatically reads environment variables and maps them to configuration properties. We use **application-{profile}.properties** files with placeholders like `${VARIABLE_NAME}` that Spring Boot resolves at runtime.

---

## 🔧 Configuration by Environment

### 1. Local Development

**Using IntelliJ IDEA:**
1. Go to `Run > Edit Configurations`
2. Select your Spring Boot configuration
3. Add Environment Variables:
```
DATABASE_URL=jdbc:postgresql://localhost:5432/eshop_Dev
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=your_password
JWT_SECRET=your_jwt_secret_key
SPRING_PROFILES_ACTIVE=dev
```

**Using VS Code:**
Create `.vscode/launch.json`:
```json
{
  "configurations": [
    {
      "type": "java",
      "name": "Spring Boot",
      "request": "launch",
      "mainClass": "com.eshop.app.EshopApplication",
      "env": {
        "DATABASE_URL": "jdbc:postgresql://localhost:5432/eshop_Dev",
        "DATABASE_USERNAME": "postgres",
        "DATABASE_PASSWORD": "your_password",
        "SPRING_PROFILES_ACTIVE": "dev"
      }
    }
  ]
}
```

**Using Command Line (Windows PowerShell):**
```powershell
$env:DATABASE_URL="jdbc:postgresql://localhost:5432/eshop_Dev"
$env:DATABASE_USERNAME="postgres"
$env:DATABASE_PASSWORD="your_password"
$env:SPRING_PROFILES_ACTIVE="dev"

./gradlew bootRun
```

**Using Command Line (Linux/Mac):**
```bash
export DATABASE_URL=jdbc:postgresql://localhost:5432/eshop_Dev
export DATABASE_USERNAME=postgres
export DATABASE_PASSWORD=your_password
export SPRING_PROFILES_ACTIVE=dev

./gradlew bootRun
```

---

### 2. Docker Compose

**Edit `docker-compose.yml`:**
```yaml
services:
  app:
    image: eshop-backend:latest
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - DATABASE_URL=jdbc:postgresql://postgres:5432/eshop_db
      - DATABASE_USERNAME=postgres
      - DATABASE_PASSWORD=secure_password
      - REDIS_HOST=redis
      - REDIS_PORT=6379
      - KEYCLOAK_ISSUER_URI=http://keycloak:8080/realms/eshop
      - JWT_SECRET=your_jwt_secret_minimum_256_bits
```

**Or use .env file (Docker Compose only):**

Create `.env` file in same directory as `docker-compose.yml`:
```env
SPRING_PROFILES_ACTIVE=docker
DATABASE_URL=jdbc:postgresql://postgres:5432/eshop_db
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=secure_password
JWT_SECRET=your_jwt_secret
```

Then in `docker-compose.yml`:
```yaml
services:
  app:
    env_file:
      - .env
```

**⚠️ Important:** Add `.env` to `.gitignore`!

---

### 3. Production Deployment

#### A. AWS EC2 / Traditional Server

**Set system environment variables:**

**Linux:**
```bash
# Add to /etc/environment or ~/.bashrc
export SPRING_PROFILES_ACTIVE=prod
export DATABASE_URL=jdbc:postgresql://prod-db-host:5432/eshop_prod
export DATABASE_USERNAME=eshop_user
export DATABASE_PASSWORD=super_secure_password
export JWT_SECRET=production_jwt_secret_key_minimum_256_bits
export REDIS_HOST=prod-redis-host
export STRIPE_SECRET_KEY=sk_live_xxxx
export RAZORPAY_KEY_ID=rzp_live_xxxx
export KEYCLOAK_ISSUER_URI=https://auth.yourdomain.com/realms/eshop
```

**Run application:**
```bash
java -jar eshop-backend.jar
```

#### B. AWS Elastic Beanstalk

Add environment variables in AWS Console:
1. Go to Configuration > Software
2. Add Environment Properties:
   - `SPRING_PROFILES_ACTIVE` = `prod`
   - `DATABASE_URL` = `jdbc:postgresql://...`
   - `DATABASE_USERNAME` = `eshop_user`
   - etc.

#### C. AWS ECS / Fargate

**In Task Definition JSON:**
```json
{
  "containerDefinitions": [
    {
      "name": "eshop-backend",
      "environment": [
        {"name": "SPRING_PROFILES_ACTIVE", "value": "prod"},
        {"name": "DATABASE_URL", "value": "jdbc:postgresql://..."}
      ],
      "secrets": [
        {
          "name": "DATABASE_PASSWORD",
          "valueFrom": "arn:aws:secretsmanager:region:account:secret:db-password"
        },
        {
          "name": "JWT_SECRET",
          "valueFrom": "arn:aws:secretsmanager:region:account:secret:jwt-secret"
        }
      ]
    }
  ]
}
```

#### D. Azure App Service

**Using Azure CLI:**
```bash
az webapp config appsettings set --name eshop-backend \
  --resource-group eshop-rg \
  --settings \
    SPRING_PROFILES_ACTIVE=prod \
    DATABASE_URL=jdbc:postgresql://... \
    DATABASE_USERNAME=eshop_user
```

**Using Azure Portal:**
1. Go to App Service > Configuration > Application Settings
2. Add New Application Setting for each variable

#### E. Kubernetes

**Create ConfigMap:**
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: eshop-config
data:
  SPRING_PROFILES_ACTIVE: "prod"
  DATABASE_URL: "jdbc:postgresql://postgres-service:5432/eshop_prod"
  DATABASE_USERNAME: "eshop_user"
  REDIS_HOST: "redis-service"
```

**Create Secret:**
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: eshop-secrets
type: Opaque
stringData:
  DATABASE_PASSWORD: "super_secure_password"
  JWT_SECRET: "production_jwt_secret_key"
  STRIPE_SECRET_KEY: "sk_live_xxxx"
```

**Use in Deployment:**
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: eshop-backend
spec:
  template:
    spec:
      containers:
      - name: eshop-backend
        image: eshop-backend:latest
        envFrom:
        - configMapRef:
            name: eshop-config
        - secretRef:
            name: eshop-secrets
```

---

## 🔐 Secrets Management Best Practices

### 1. AWS Secrets Manager
```java
// Spring Cloud AWS automatically integrates
// Add to pom.xml: spring-cloud-starter-aws-secrets-manager-config
// Secrets are loaded at startup
```

### 2. Azure Key Vault
```java
// Add to pom.xml: azure-spring-boot-starter-keyvault-secrets
// Configure in application.properties:
// azure.keyvault.uri=https://your-vault.vault.azure.net/
```

### 3. HashiCorp Vault
```java
// Add to pom.xml: spring-cloud-starter-vault-config
// Configure in bootstrap.properties
```

---

## 📝 Required Environment Variables

### Core Application
```
SPRING_PROFILES_ACTIVE=dev|test|prod|docker
```

### Database
```
DATABASE_URL=jdbc:postgresql://host:5432/database_name
DATABASE_USERNAME=username
DATABASE_PASSWORD=password
```

### Redis Cache
```
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=optional_password
```

### Authentication (Keycloak)
```
KEYCLOAK_ISSUER_URI=http://keycloak:8080/realms/eshop
KEYCLOAK_JWK_URI=http://keycloak:8080/realms/eshop/protocol/openid-connect/certs
```

### JWT (if not using Keycloak)
```
JWT_SECRET=minimum_256_bits_random_string
JWT_EXPIRATION=3600000
JWT_REFRESH_EXPIRATION=604800000
```

### Payment Gateways (Production)
```
STRIPE_SECRET_KEY=sk_live_xxxx
STRIPE_PUBLIC_KEY=pk_live_xxxx
RAZORPAY_KEY_ID=rzp_live_xxxx
RAZORPAY_KEY_SECRET=xxxx
```

### CORS
```
CORS_ORIGINS=https://yourdomain.com,https://www.yourdomain.com
```

---

## ✅ Security Checklist

- [ ] Never commit `.env` files with real credentials to Git
- [ ] Always use `.env.example` as a template
- [ ] Use secrets managers in production (AWS Secrets Manager, Azure Key Vault)
- [ ] Rotate secrets regularly
- [ ] Use different credentials for dev/test/prod
- [ ] Restrict database access by IP/network
- [ ] Use IAM roles instead of hardcoded credentials when possible
- [ ] Enable encryption at rest and in transit
- [ ] Monitor secret access logs
- [ ] Use least privilege principle for service accounts

---

## 🔗 References

- [Spring Boot External Configuration](https://docs.spring.io/spring-boot/reference/features/external-config.html)
- [AWS Secrets Manager Integration](https://docs.awspring.io/spring-cloud-aws/docs/current/reference/html/index.html#integrating-your-spring-cloud-application-with-the-aws-secrets-manager)
- [Azure Key Vault Integration](https://learn.microsoft.com/en-us/azure/developer/java/spring-framework/configure-spring-boot-starter-java-app-with-azure-key-vault)
- [12-Factor App: Config](https://12factor.net/config)


# --- File: jwt-quick-start.md ---

# 🚀 Quick Start: JWT Authentication Setup

## ⚡ Files Changed

1. ✅ **`Roles.java`** - New role constants class
2. ✅ **`OAuth2SecurityConfig.java`** - Reads from `"roles"` claim
3. ✅ **`DashboardController.java`** - Updated `@PreAuthorize` to allow ADMIN
4. ✅ **`MeController.java`** - Enhanced JWT debugging endpoint

## 🔥 What Works NOW

### ✅ JWT Token Validation
- Spring automatically validates JWT from Keycloak
- No custom filters needed
- Stateless authentication

### ✅ Role Extraction
Reads roles directly from JWT `"roles"` claim:
```json
{
  "roles": ["SELLER", "ADMIN"]
}
```

### ✅ Authorization
- `@PreAuthorize("hasRole('ADMIN')")` → Admin only
- `@PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")` → Seller OR Admin

## 🔴 ONE CRITICAL STEP LEFT: Keycloak Mapper

**Go to Keycloak Admin Console:**

1. **Client Scopes** → `roles` → **Mappers** → **Add mapper**
2. Select **"User Realm Role"**
3. Configure:
   - **Token Claim Name:** `roles` ⚠️
   - **Multivalued:** `ON` ✅
   - **Add to access token:** `ON` ✅

**That's it!** ✅

## 🧪 Test It

```bash
# Get token
TOKEN=$(curl -X POST "http://localhost:8080/realms/eshop/protocol/openid-connect/token" \
  -d "client_id=eshop-client" \
  -d "client_secret=YOUR_SECRET" \
  -d "username=seller@test.com" \
  -d "password=password" \
  -d "grant_type=password" | jq -r '.access_token')

# Test /me endpoint
curl -H "Authorization: Bearer $TOKEN" http://localhost:8082/api/me | jq

# Test seller dashboard
curl -H "Authorization: Bearer $TOKEN" http://localhost:8082/api/v1/dashboard/seller | jq
```

## ✅ Expected Response from /me

```json
{
  "sub": "user-id",
  "userId": "user-id",
  "username": "seller@test.com",
  "email": "seller@test.com",
  "roles": ["SELLER"],
  "authorities": ["ROLE_SELLER"],
  "tokenIssuedAt": "2026-01-01T10:00:00Z",
  "tokenExpiresAt": "2026-01-01T11:00:00Z"
}
```

## 🎯 Endpoint Access Control

| Endpoint | ADMIN | SELLER | CUSTOMER |
|----------|-------|--------|----------|
| `/api/v1/dashboard/admin` | ✅ | ❌ | ❌ |
| `/api/v1/dashboard/seller` | ✅ | ✅ | ❌ |
| `/api/v1/dashboard/customer` | ❌ | ❌ | ✅ |
| `/api/me` | ✅ | ✅ | ✅ |

## 🔧 Debugging Tips

**No roles in JWT?**
→ Check Keycloak mapper configuration

**401 Unauthorized?**
→ Check token expiration, verify Authorization header

**403 Forbidden?**
→ User doesn't have required role, check `/api/me`

## 📝 Key Files to Review

- [JWT_AUTHENTICATION_IMPLEMENTATION.md](JWT_AUTHENTICATION_IMPLEMENTATION.md) - Full documentation
- `src/main/java/com/eshop/app/constants/Roles.java` - Role constants
- `src/main/java/com/eshop/app/config/OAuth2SecurityConfig.java` - Security config
- `src/main/java/com/eshop/app/controller/MeController.java` - Debug endpoint

---

**Status:** ✅ Implementation Complete | ⚠️ Keycloak mapper pending  
**Build:** ✅ Successful  
**Date:** January 1, 2026


# --- File: KEYCLOAK_SETUP.md ---

# Keycloak Setup and Configuration Guide

## Table of Contents
1. [Prerequisites](#prerequisites)
2. [Installation](#installation)
3. [Initial Configuration](#initial-configuration)
4. [Realm Setup](#realm-setup)
5. [Client Configuration](#client-configuration)
6. [Role Configuration](#role-configuration)
7. [User Management](#user-management)
8. [Testing](#testing)
9. [Troubleshooting](#troubleshooting)

---

## Prerequisites

### Required Software
- Docker and Docker Compose (recommended)
- OR Standalone Keycloak installation
- PostgreSQL database
- Java 17+ (for backend integration)

### Required Knowledge
- Basic understanding of OAuth2/OIDC
- Docker basics
- REST API concepts

---

## Installation

### Option 1: Docker Compose (Recommended)

The project includes Keycloak in `docker-compose.yml`:

```yaml
keycloak:
  image: quay.io/keycloak/keycloak:23.0
  container_name: keycloak
  environment:
    KC_DB: postgres
    KC_DB_URL: jdbc:postgresql://postgres:5432/keycloak
    KC_DB_USERNAME: keycloak
    KC_DB_PASSWORD: keycloak
    KEYCLOAK_ADMIN: admin
    KEYCLOAK_ADMIN_PASSWORD: admin
    KC_HOSTNAME: localhost
    KC_HTTP_ENABLED: true
  ports:
    - "8080:8080"
  command:
    - start-dev
    - --import-realm
  volumes:
    - ./realm-export.json:/opt/keycloak/data/import/realm-export.json
  depends_on:
    - postgres
  networks:
    - eshop-network
```

**Start Keycloak:**
```bash
cd G:\Project\eshop_back
docker-compose up -d keycloak
```

**Verify:**
```bash
# Check if Keycloak is running
docker ps | grep keycloak

# Check logs
docker logs keycloak

# Access admin console
# URL: http://localhost:8080
# Username: admin
# Password: admin
```

### Option 2: Standalone Installation

Download from: https://www.keycloak.org/downloads

```bash
# Extract
unzip keycloak-23.0.zip
cd keycloak-23.0

# Set admin credentials
export KEYCLOAK_ADMIN=admin
export KEYCLOAK_ADMIN_PASSWORD=admin

# Start in dev mode
./bin/kc.sh start-dev
```

---

## Initial Configuration

### 1. Access Admin Console

**URL:** http://localhost:8080

**Default Credentials:**
- Username: `admin`
- Password: `admin`

⚠️ **Important:** Change the admin password in production!

### 2. Create Admin User (if needed)

If accessing for the first time:
1. Navigate to http://localhost:8080
2. Click "Administration Console"
3. Create admin credentials
4. Login

---

## Realm Setup

### Automatic Setup (Recommended)

The project includes a pre-configured realm export file: `realm-export.json`

**Import the realm:**

```bash
# Using Docker
docker exec -it keycloak /opt/keycloak/bin/kc.sh import \
  --file /opt/keycloak/data/import/realm-export.json \
  --override true

# OR restart with import flag (already configured in docker-compose)
docker-compose restart keycloak
```

**Verify import:**
1. Login to admin console
2. Top-left dropdown should show "eshop" realm
3. Verify roles exist: Customer, Seller, DELIVERY_AGENT

### Manual Realm Creation

If you need to create from scratch:

1. **Create Realm**
   - Admin Console → Click "master" dropdown (top-left)
   - Click "Create Realm"
   - Realm name: `eshop`
   - Enabled: ON
   - Click "Create"

2. **Configure Realm Settings**
   - Select "eshop" realm
   - Go to: Realm Settings

3. **General Settings**
   ```
   Display name: E-Shop
   HTML Display name: <b>E-Shop</b>
   Frontend URL: (leave empty for dev)
   Require SSL: None (for dev), External requests (for prod)
   ```

4. **Login Settings**
   - Go to: Realm Settings → Login
   - Configure:
     ```
     User registration: ON
     Forgot password: ON
     Remember me: ON
     Verify email: OFF (for dev), ON (for prod)
     Login with email: ON
     Duplicate emails: OFF
     ```

5. **User Registration Settings**
   - Go to: Realm Settings → User Registration
   - Default Roles: Add "Customer"
   - This ensures new users automatically get CUSTOMER role

6. **Themes (Optional)**
   - Login theme: keycloak
   - Account theme: keycloak
   - Admin theme: keycloak
   - Email theme: keycloak

7. **Tokens**
   - Go to: Realm Settings → Tokens
   - Configure:
     ```
     Access Token Lifespan: 5 minutes
     SSO Session Idle: 30 minutes
     SSO Session Max: 10 hours
     Refresh Token Max Reuse: 0
     ```

---

## Client Configuration

### Client 1: Frontend Client (Public)

**Purpose:** For frontend application (Next.js)

1. **Create Client**
   - Clients → Create client
   - Client ID: `eshop-client`
   - Client type: OpenID Connect
   - Click "Next"

2. **Capability config**
   ```
   Client authentication: OFF (public client)
   Authorization: OFF
   Standard flow: ON (OAuth2 Authorization Code)
   Direct access grants: OFF (use standard flow)
   Implicit flow: OFF (deprecated)
   Service accounts: OFF
   ```

3. **Access Settings**
   ```
   Root URL: http://localhost:3000
   Home URL: http://localhost:3000
   Valid redirect URIs: 
     - http://localhost:3000/*
     - http://localhost:4200/*
     - http://localhost:5173/*
   Valid post logout redirect URIs: 
     - http://localhost:3000/*
   Web origins: 
     - http://localhost:3000
     - http://localhost:4200
     - http://localhost:5173
   ```

4. **Advanced Settings**
   ```
   PKCE: S256 (required)
   Frontchannel logout: ON
   ```

### Client 2: Backend Service Account (Confidential)

**Purpose:** For backend to manage users/roles via Keycloak Admin API

1. **Create Client**
   - Clients → Create client
   - Client ID: `eshop-backend`
   - Client type: OpenID Connect
   - Click "Next"

2. **Capability config**
   ```
   Client authentication: ON (confidential)
   Authorization: OFF
   Standard flow: OFF
   Direct access grants: OFF
   Implicit flow: OFF
   Service accounts: ON
   ```

3. **Credentials**
   - Go to: Clients → eshop-backend → Credentials
   - Client Authenticator: Client Id and Secret
   - Copy the secret (e.g., `aWHhjsbAeg8LeeTvtkDerrCQGhEuJ5ph`)
   - Save this in backend `application.properties`

4. **Service Account Roles**
   - Go to: Clients → eshop-backend → Service Account Roles
   - Click "Assign role"
   - Filter by clients: Select "realm-management"
   - Assign these roles:
     - `manage-users`
     - `view-users`
     - `manage-realm`
     - `view-realm`

   This allows the backend to assign roles to users.

### Client Scopes and Mappers

**Ensure roles are included in JWT:**

1. **Go to:** Clients → eshop-client → Client Scopes

2. **Add roles mapper:**
   - Click "eshop-client-dedicated" scope
   - Go to "Mappers" tab
   - Click "Add mapper" → "By configuration"
   - Select "User Realm Role"
   
   Configure:
   ```
   Name: realm roles
   Mapper Type: User Realm Role
   Token Claim Name: realm_access.roles
   Claim JSON Type: String
   Add to ID token: ON
   Add to access token: ON
   Add to userinfo: ON
   Multivalued: ON
   ```

3. **Add username mapper:**
   - Add mapper → "User Property"
   ```
   Name: username
   Mapper Type: User Property
   Property: username
   Token Claim Name: preferred_username
   Claim JSON Type: String
   Add to ID token: ON
   Add to access token: ON
   Add to userinfo: ON
   ```

---

## Role Configuration

### Create Realm Roles

1. **Navigate to Roles**
   - Select "eshop" realm
   - Go to: Realm roles

2. **Create CUSTOMER Role**
   - Click "Create role"
   - Role name: `Customer`
   - Description: `Customer role for browsing and purchasing`
   - Click "Save"

3. **Create SELLER Role**
   - Click "Create role"
   - Role name: `Seller`
   - Description: `Seller role for managing products and shops`
   - Click "Save"

4. **Create DELIVERY_AGENT Role**
   - Click "Create role"
   - Role name: `DELIVERY_AGENT`
   - Description: `Delivery agent role for managing deliveries`
   - Click "Save"

5. **Create ADMIN Role** (Optional)
   - Click "Create role"
   - Role name: `ADMIN`
   - Description: `Administrator role with full access`
   - Click "Save"

### Set Default Role (IMPORTANT)

This ensures all new users get CUSTOMER role automatically:

1. **Go to:** Realm Settings → User Registration
2. **Default Roles section**
3. Click "Assign role"
4. Select "Customer"
5. Click "Assign"
6. **Save**

**Verify:**
- Go to: Realm Settings → User Registration
- Default Roles should show: `Customer`

---

## User Management

### Create Test Users

**User 1: Regular Customer**

1. Go to: Users → Create user
2. Configure:
   ```
   Username: customer
   Email: customer@eshop.com
   Email verified: ON (for dev)
   First name: Regular
   Last name: Customer
   Enabled: ON
   ```
3. Click "Create"
4. Go to: Credentials tab
   - Click "Set password"
   - Password: `customer`
   - Temporary: OFF
   - Click "Save"
5. Go to: Role mappings tab
   - Verify "Customer" role is assigned (should be automatic)

**User 2: Seller**

1. Create user with:
   ```
   Username: seller
   Email: seller@eshop.com
   First name: Seller
   Last name: User
   Password: seller
   ```
2. Assign roles:
   - Customer (should be automatic)
   - Seller (assign manually)

**User 3: Admin**

1. Create user with:
   ```
   Username: admin
   Email: admin@eshop.com
   First name: Admin
   Last name: User
   Password: admin
   ```
2. Assign roles:
   - Customer
   - Seller
   - ADMIN

### User Attributes (Optional)

You can add custom attributes to users:

1. Go to: Users → Select user → Attributes
2. Add custom attributes:
   - Key: `phone`
   - Value: `+1234567890`
3. Click "Save"

These attributes will be included in JWT if you create a mapper for them.

---

## Testing

### Test 1: Verify Keycloak is Running

```bash
# Check health
curl http://localhost:8080/health

# Get OpenID configuration
curl http://localhost:8080/realms/eshop/.well-known/openid-configuration | jq
```

### Test 2: Test User Login

```bash
# Login with password grant (requires direct access grants enabled)
curl -X POST http://localhost:8080/realms/eshop/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=eshop-client" \
  -d "username=customer" \
  -d "password=customer" \
  -d "scope=openid profile email" | jq

# Response includes:
# - access_token
# - refresh_token
# - id_token
```

### Test 3: Verify Roles in JWT

```bash
# Decode access token (copy from above response)
echo "<access_token>" | cut -d. -f2 | base64 -d | jq

# Should include:
# {
#   "realm_access": {
#     "roles": ["Customer"]
#   },
#   "preferred_username": "customer",
#   ...
# }
```

### Test 4: Test Service Account

```bash
# Get service account token
curl -X POST http://localhost:8080/realms/eshop/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials" \
  -d "client_id=eshop-backend" \
  -d "client_secret=aWHhjsbAeg8LeeTvtkDerrCQGhEuJ5ph" | jq

# Use token to list users
TOKEN="<service_account_token>"
curl http://localhost:8080/admin/realms/eshop/users \
  -H "Authorization: Bearer $TOKEN" | jq
```

### Test 5: Test Role Assignment

```bash
# Get user ID
USER_ID=$(curl http://localhost:8080/admin/realms/eshop/users?username=customer \
  -H "Authorization: Bearer $TOKEN" | jq -r '.[0].id')

# Get Seller role
ROLE=$(curl http://localhost:8080/admin/realms/eshop/roles/Seller \
  -H "Authorization: Bearer $TOKEN")

# Assign Seller role to customer
curl -X POST http://localhost:8080/admin/realms/eshop/users/$USER_ID/role-mappings/realm \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "[$ROLE]"

# Verify
curl http://localhost:8080/admin/realms/eshop/users/$USER_ID/role-mappings/realm \
  -H "Authorization: Bearer $TOKEN" | jq
```

---

## Troubleshooting

### Issue: Cannot Access Admin Console

**Symptoms:** http://localhost:8080 not accessible

**Solutions:**

1. **Check if Keycloak is running:**
   ```bash
   docker ps | grep keycloak
   ```

2. **Check logs:**
   ```bash
   docker logs keycloak
   ```

3. **Check port binding:**
   ```bash
   netstat -an | grep 8080
   # or
   Get-NetTCPConnection -LocalPort 8080
   ```

4. **Restart Keycloak:**
   ```bash
   docker-compose restart keycloak
   ```

### Issue: Realm Not Found

**Symptoms:** Frontend redirects fail, "Realm not found" error

**Solutions:**

1. **Verify realm exists:**
   ```bash
   curl http://localhost:8080/realms/eshop/.well-known/openid-configuration
   ```

2. **Check realm name in configuration:**
   - Backend `application.properties`: `keycloak.realm=eshop`
   - Frontend `.env`: `KEYCLOAK_REALM=eshop`

3. **Re-import realm:**
   ```bash
   docker exec -it keycloak /opt/keycloak/bin/kc.sh import \
     --file /opt/keycloak/data/import/realm-export.json \
     --override true
   ```

### Issue: Client Not Found

**Symptoms:** "Client not found" during login

**Solutions:**

1. **Verify client exists:**
   - Admin Console → Clients
   - Check for "eshop-client" and "eshop-backend"

2. **Check client ID in code:**
   - Frontend: `KEYCLOAK_CLIENT_ID=eshop-client`
   - Backend: `keycloak.resource=eshop-backend`

3. **Recreate client** if missing (follow Client Configuration section)

### Issue: Invalid Redirect URI

**Symptoms:** "Invalid redirect_uri" error during login

**Solutions:**

1. **Check Valid Redirect URIs in Keycloak:**
   - Clients → eshop-client → Settings
   - Valid redirect URIs should include:
     - `http://localhost:3000/*`
     - Your frontend URL with wildcard

2. **Check redirect_uri in frontend request:**
   - Must exactly match one of the configured URIs

3. **Common mistake:** Missing trailing `/*` in Keycloak configuration

### Issue: Roles Not in JWT

**Symptoms:** JWT doesn't include `realm_access.roles`

**Solutions:**

1. **Check client scope mappers:**
   - Clients → eshop-client → Client Scopes → eshop-client-dedicated → Mappers
   - Ensure "realm roles" mapper exists

2. **Add roles mapper** (see Client Configuration section)

3. **Request correct scopes:**
   ```
   scope=openid profile email
   ```

### Issue: Service Account Can't Manage Users

**Symptoms:** 403 Forbidden when assigning roles

**Solutions:**

1. **Check service account roles:**
   - Clients → eshop-backend → Service Account Roles
   - Must have: `manage-users`, `view-users`, `manage-realm`

2. **Assign missing roles:**
   - Filter by clients: realm-management
   - Assign required roles

3. **Verify client secret:**
   - Check `application.properties`: `keycloak.credentials.secret`
   - Must match: Clients → eshop-backend → Credentials

### Issue: Default Role Not Assigned

**Symptoms:** New users don't have Customer role

**Solutions:**

1. **Check default roles configuration:**
   - Realm Settings → User Registration → Default Roles
   - "Customer" should be listed

2. **Add default role:**
   - Click "Assign role"
   - Select "Customer"
   - Save

3. **Test with new user:**
   - Register new user
   - Check Role Mappings tab
   - Should have "Customer" role

---

## Production Considerations

### Security

1. **Change Admin Password**
   ```bash
   # Via Admin Console
   # Or environment variable
   KEYCLOAK_ADMIN_PASSWORD=<strong-password>
   ```

2. **Enable HTTPS**
   ```yaml
   # docker-compose.yml
   KC_HTTPS_ENABLED: true
   KC_HTTPS_CERTIFICATE_FILE: /path/to/cert.pem
   KC_HTTPS_CERTIFICATE_KEY_FILE: /path/to/key.pem
   ```

3. **Enable Email Verification**
   - Realm Settings → Login → Verify email: ON
   - Configure SMTP settings

4. **Configure SMTP**
   - Realm Settings → Email
   ```
   From: noreply@yourdomain.com
   SMTP Host: smtp.gmail.com
   SMTP Port: 587
   Enable StartTLS: ON
   Enable Authentication: ON
   Username: your-email@gmail.com
   Password: app-specific-password
   ```

5. **Enable SSL Required**
   - Realm Settings → General
   - Require SSL: External requests

### Database

1. **Use External PostgreSQL**
   ```yaml
   KC_DB_URL: jdbc:postgresql://production-db:5432/keycloak
   KC_DB_USERNAME: keycloak_user
   KC_DB_PASSWORD: <strong-password>
   ```

2. **Database Backups**
   ```bash
   # Backup Keycloak database
   pg_dump -h localhost -U keycloak_user keycloak > keycloak_backup.sql
   ```

### Scaling

1. **Clustered Setup**
   - Use external cache (Infinispan)
   - Configure load balancer
   - Shared database

2. **Performance Tuning**
   ```
   KC_DB_POOL_INITIAL_SIZE: 10
   KC_DB_POOL_MAX_SIZE: 50
   ```

### Monitoring

1. **Health Checks**
   ```bash
   curl http://localhost:8080/health
   curl http://localhost:8080/metrics
   ```

2. **Logging**
   ```bash
   # Configure log level
   KC_LOG_LEVEL: INFO
   ```

---

## Configuration Files Reference

### Backend: application.properties

```properties
# Keycloak Configuration
keycloak.auth-server-url=http://localhost:8080
keycloak.realm=eshop
keycloak.resource=eshop-backend
keycloak.credentials.secret=aWHhjsbAeg8LeeTvtkDerrCQGhEuJ5ph

# For admin operations
keycloak.admin.clientId=eshop-backend
keycloak.admin.username=admin
keycloak.admin.password=admin
```

### Frontend: .env

```env
KEYCLOAK_URL=http://localhost:8080
KEYCLOAK_REALM=eshop
KEYCLOAK_CLIENT_ID=eshop-client
NEXT_PUBLIC_KEYCLOAK_URL=http://localhost:8080
NEXT_PUBLIC_KEYCLOAK_REALM=eshop
```

### Docker Compose: docker-compose.yml

```yaml
keycloak:
  image: quay.io/keycloak/keycloak:23.0
  environment:
    KC_DB: postgres
    KC_DB_URL: jdbc:postgresql://postgres:5432/keycloak
    KC_DB_USERNAME: keycloak
    KC_DB_PASSWORD: keycloak
    KEYCLOAK_ADMIN: admin
    KEYCLOAK_ADMIN_PASSWORD: admin
    KC_HOSTNAME: localhost
    KC_HTTP_ENABLED: true
  ports:
    - "8080:8080"
  command:
    - start-dev
    - --import-realm
  volumes:
    - ./realm-export.json:/opt/keycloak/data/import/realm-export.json
```

---

## Quick Reference Commands

```bash
# Start Keycloak
docker-compose up -d keycloak

# Stop Keycloak
docker-compose stop keycloak

# View logs
docker logs -f keycloak

# Access bash
docker exec -it keycloak bash

# Import realm
docker exec -it keycloak /opt/keycloak/bin/kc.sh import \
  --file /opt/keycloak/data/import/realm-export.json

# Export realm
docker exec -it keycloak /opt/keycloak/bin/kc.sh export \
  --realm eshop \
  --file /tmp/realm-export.json

# Copy exported realm
docker cp keycloak:/tmp/realm-export.json ./realm-export.json

# Restart Keycloak
docker-compose restart keycloak

# Remove Keycloak (data will be lost)
docker-compose down keycloak
docker volume rm eshop_keycloak_data
```

---

## Next Steps

After completing Keycloak setup:

1. ✅ Configure backend to use Keycloak (see `application.properties`)
2. ✅ Configure frontend OAuth2 flow
3. ✅ Test user registration and login
4. ✅ Test role assignment
5. ✅ Review [Role Management Guide](../guides/ROLE_MANAGEMENT.md)

---

**Document Version:** 1.0  
**Last Updated:** 2026-02-17  
**Author:** Development Team


# --- File: local-dev-setup.md ---

# 🔧 Local Development Setup (Without Docker)

If you want to run the backend **locally** (not in Docker) but still use Redis:

---

## Option 1: Redis on Windows (Recommended)

### Using WSL2 (Easiest)
```powershell
# Install Redis in WSL2
wsl
sudo apt update
sudo apt install redis-server
redis-server
```

### Using Docker for Redis Only
```powershell
# Start only Redis container
docker run -d --name redis -p 6379:6379 redis:7-alpine

# Verify
docker ps
```

### Using Memurai (Native Windows Redis)
Download from: https://www.memurai.com/

---

## Option 2: Disable Redis for Local Dev

### Quick Fix: Disable Redis Cache

**In `application-dev.properties`:**
```properties
# Disable Redis - Use in-memory cache instead
spring.cache.type=caffeine
app.redis.enabled=false
```

**Or set environment variable:**
```powershell
$env:SPRING_CACHE_TYPE="caffeine"
.\gradlew.bat bootRun --args="--spring.profiles.active=dev"
```

---

## Option 3: Make Redis Optional

### Graceful Degradation (Enterprise Pattern)

Already implemented in your app! Check:
```properties
# application.properties
app.redis.resilient.mode=true
```

This means:
- ✅ Redis available → Use Redis
- ✅ Redis down → Fallback to Caffeine (local cache)
- ✅ No app crash

---

## Run Locally

```powershell
# Method 1: Using Gradle
.\gradlew.bat bootRun --args="--spring.profiles.active=dev"

# Method 2: Using JAR
.\gradlew.bat clean build -x test
java -jar build/libs/eshop-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev

# Method 3: IDE (IntelliJ/VS Code)
# Set VM Options: -Dspring.profiles.active=dev
```

---

## Profile Selection Guide

| Profile | Use Case | Redis Host | Database |
|---------|----------|------------|----------|
| **dev** | Local development | `localhost` | localhost:5432 |
| **docker** | Docker containers | `redis` (service name) | postgres:5432 |
| **prod** | Production | Redis cluster | Cloud DB |
| **test** | Unit tests | Mock/Embedded | H2/TestContainers |

---

## Quick Redis Commands

```bash
# Check if Redis is running
redis-cli ping
# Expected: PONG

# Monitor Redis activity
redis-cli monitor

# Check keys
redis-cli keys "*"

# Clear all cache
redis-cli flushall
```

---

## Environment Variables for Local Dev

```powershell
# PowerShell
$env:SPRING_PROFILES_ACTIVE="dev"
$env:REDIS_HOST="localhost"
$env:DATABASE_URL="jdbc:postgresql://localhost:5432/eshop_Dev"

# Then run
.\gradlew.bat bootRun
```

---

## Summary

**For Docker:** Use `docker-compose.yml` → All services managed  
**For Local Dev:** Use `dev` profile → Redis on localhost or disabled  

Choose what works best for your workflow! 🚀

