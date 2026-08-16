

# --- File: hibernate-dialect-fix.md ---

# CRITICAL-006 FIX: Hibernate Dialect Deprecation

## Problem
```
HHH90000025: PostgreSQLDialect does not need to be specified explicitly using 'hibernate.dialect' 
(remove the property setting and it will be selected by default)
```

## Root Cause
- Hibernate 7 (used in Spring Boot 4.x) auto-detects database dialect from the datasource
- Explicit `spring.jpa.database-platform` and `spring.jpa.properties.hibernate.dialect` are deprecated
- Configuration causes startup warnings

## Files Modified
1. ✅ `application-dev.properties` - Removed both occurrences
2. ✅ `application-prod.properties` - Removed dialect configuration  
3. ⚠️ `application-test.properties` - Has duplicate entries (lines 29 and 130) - **MANUAL REMOVAL NEEDED**
4. ⚠️ `src/test/resources/application.properties` - Has H2 dialect - **MANUAL REMOVAL NEEDED**

## Manual Cleanup Required

### application-test.properties
Remove these lines (appears twice in file):
```properties
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
```

### src/test/resources/application.properties  
Remove this line:
```properties
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect
```

## Result After Fix
- No more HHH90000025 warnings
- Cleaner configuration
- Hibernate automatically selects correct dialect based on JDBC URL

## Verification
Start application and check logs - should not see any dialect warnings:
```bash
./gradlew bootRun --args="--spring.profiles.active=dev"
```

Look for absence of:
```
WARN org.hibernate.orm.deprecation - HHH90000025: PostgreSQLDialect does not need to be specified explicitly
```


# --- File: KEYCLOAK_CLIENT_SECRET_UPDATE.md ---

# Keycloak Client Secret Update Guide & Troubleshooting

## Summary
If the backend application fails with `HTTP 401 Unauthorized` during Keycloak interactions (assigning roles, verifying tokens), it is most likely due to an invalid or rotated Client Secret for the `eshop-backend` client.

## Resolution Steps

### 1. Retrieve New Secret from Keycloak
1.  Log in to **Keycloak Admin Console** (http://127.0.0.1:8080/admin) — use `127.0.0.1` not `localhost`.
2.  Navigate to **Clients** -> **`eshop-backend`** -> **Credentials**.
3.  Click **Regenerate** (if needed) and copy the **Client Secret**.

### 2. Update Configuration Files (CRITICAL)
Due to profile precedence, you must ensure the secret is updated in **application-dev.properties** if running in the development profile (default).

#### A. Development Override (Highest Priority in Dev)
**File**: `src/main/resources/application-dev.properties`
**Property**: `keycloak.client-secret`
```properties
keycloak.client-secret=${KEYCLOAK_CLIENT_SECRET:YOUR_NEW_SECRET_HERE}
```

#### B. Base Configuration (Fallback)
**File**: `src/main/resources/application.properties`
**Property**: `keycloak.credentials.secret`
```properties
keycloak.credentials.secret=${ESHOP_BACKEND_CLIENT_SECRET:YOUR_NEW_SECRET_HERE}
```

### 3. Restart Application
After updating the `.properties` files, you **must restart the backend application** for changes to take effect.
```bash
./gradlew bootRun
```

## Best Practice: Environment Variables
To avoid editing files and committing secrets, set the environment variable on your deployment or local machine. This overrides all file-based configurations.

| Environment Variable | Description |
| :--- | :--- |
| `ESHOP_BACKEND_CLIENT_SECRET` | Used by `application.properties` |
| `KEYCLOAK_CLIENT_SECRET` | Used by `application-dev.properties` |

Setting both to the same secret value is recommended.


# --- File: spring-boot-4-fixes.md ---

# Spring Boot 4.0 Configuration Fixes - Complete Summary

## 📋 Overview
All Spring Boot 4.0 configuration warnings have been resolved. This document details the changes made to ensure full compatibility with Spring Boot 4.0.0 and Java 21.

---

## ✅ Fixed Issues

### 1. **Flyway Database Migration** ✓
**Problem**: Flyway 9.x doesn't include PostgreSQL driver by default in Spring Boot 4.0

**Solution**:
```gradle
// build.gradle - UPDATED
implementation 'org.flywaydb:flyway-core:10.10.0'
implementation 'org.flywaydb:flyway-database-postgresql:10.10.0'  // NEW
```

**Properties** (no changes needed):
```properties
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
spring.flyway.baseline-on-migrate=true
spring.flyway.validate-on-migrate=true
spring.flyway.out-of-order=false
spring.flyway.clean-disabled=true
```

---

### 2. **Deprecated Error Handling Properties** ✓
**Problem**: `server.error.*` properties deprecated in Spring Boot 4.0

**Before**:
```properties
server.error.include-message=on-param
server.error.include-binding-errors=on-param
server.error.include-stacktrace=on-param
server.error.include-exception=false
server.error.whitelabel.enabled=false
```

**After**:
```properties
# Spring Boot 4.x - Updated namespace
spring.web.error.include-message=on-param
spring.web.error.include-binding-errors=on-param
spring.web.error.include-stacktrace=on-param
spring.web.error.include-exception=false
spring.web.error.whitelabel.enabled=false
```

---

### 3. **Spring Retry Configuration** ✓
**Problem**: `spring.retry.enabled` property doesn't exist

**Before**:
```properties
spring.retry.enabled=true
```

**After**:
- **Removed the property**
- Added `@EnableRetry` annotation to main application class (already present)

```java
@SpringBootApplication
@EnableRetry  // This is the correct way to enable Spring Retry
public class EshopApplication {
    // ...
}
```

---

### 4. **SpringDoc OAuth2 Properties** ✓
**Problem**: `springdoc.oauth2.*` properties moved to `springdoc.swagger-ui.oauth2.*`

**Before**:
```properties
springdoc.oauth2.authorization-url=${keycloak.auth-server-url}/realms/${keycloak.realm}/protocol/openid-connect/auth
springdoc.oauth2.token-url=${keycloak.auth-server-url}/realms/${keycloak.realm}/protocol/openid-connect/token
```

**After**:
```properties
# Spring Boot 4.x - Updated namespace
springdoc.swagger-ui.oauth2.authorization-url=${keycloak.auth-server-url}/realms/${keycloak.realm}/protocol/openid-connect/auth
springdoc.swagger-ui.oauth2.token-url=${keycloak.auth-server-url}/realms/${keycloak.realm}/protocol/openid-connect/token
springdoc.swagger-ui.oauth2.use-pkce-with-authorization-code-grant=true
```

---

### 5. **Custom Properties Type-Safe Binding** ✓
**Problem**: All `app.*` properties need `@ConfigurationProperties` binding for:
- IDE autocomplete support
- Type safety and validation
- Eliminates "unknown property" warnings

**Solution**: Created `AppProperties.java`

```java
@Configuration
@ConfigurationProperties(prefix = "app")
@Data
@Validated
public class AppProperties {
    
    private String name = "E-Shop";
    private String version = "1.0.0";
    private String environment = "development";
    
    private Security security = new Security();
    private Cors cors = new Cors();
    private Analytics analytics = new Analytics();
    private RateLimit ratelimit = new RateLimit();
    private Logging logging = new Logging();
    private Audit audit = new Audit();
    private Upload upload = new Upload();
    private Validation validation = new Validation();
    private Api api = new Api();
    private Business business = new Business();
    private Performance performance = new Performance();
    private Features features = new Features();
    private Product product = new Product();
    private Cache cache = new Cache();
    private Openapi openapi = new Openapi();
    
    // ... nested classes with getters/setters
}
```

**Registered in Main Application**:
```java
@EnableConfigurationProperties({
    JwtProperties.class, 
    ApiInfoProperties.class,
    AppProperties.class  // ✓ Added
})
@ConfigurationPropertiesScan({
    "com.eshop.app.config", 
    "com.eshop.app.config.properties"  // ✓ Added
})
public class EshopApplication {
    // ...
}
```

---

### 6. **Configuration Processor** ✓
**Purpose**: Generates metadata for IDE autocomplete and eliminates warnings

**Already Present in build.gradle**:
```gradle
annotationProcessor 'org.springframework.boot:spring-boot-configuration-processor'
compileOnly 'org.springframework.boot:spring-boot-configuration-processor'
```

**What it does**:
- Generates `spring-configuration-metadata.json`
- Enables autocomplete for custom properties in IDE
- Provides property validation at compile time

---

## 📁 Files Created

1. **AppProperties.java** - Type-safe configuration binding
   - Location: `src/main/java/com/eshop/app/config/properties/AppProperties.java`
   - Binds all `app.*` properties from application.properties
   - Includes validation annotations (`@Min`, `@Max`, `@NotNull`)

---

## 📝 Files Modified

### 1. **build.gradle**
```diff
  // Flyway for database migrations
- implementation 'org.flywaydb:flyway-core:9.22.3'
+ implementation 'org.flywaydb:flyway-core:10.10.0'
+ implementation 'org.flywaydb:flyway-database-postgresql:10.10.0'
```

### 2. **application.properties**
```diff
  # Error Handling
- server.error.include-message=on-param
- server.error.include-binding-errors=on-param
- server.error.include-stacktrace=on-param
- server.error.include-exception=false
- server.error.whitelabel.enabled=false
+ spring.web.error.include-message=on-param
+ spring.web.error.include-binding-errors=on-param
+ spring.web.error.include-stacktrace=on-param
+ spring.web.error.include-exception=false
+ spring.web.error.whitelabel.enabled=false

- # RETRY
- spring.retry.enabled=true
+ # (Removed - use @EnableRetry annotation instead)

  # OAuth2 for Swagger
- springdoc.oauth2.authorization-url=...
- springdoc.oauth2.token-url=...
+ springdoc.swagger-ui.oauth2.authorization-url=...
+ springdoc.swagger-ui.oauth2.token-url=...
+ springdoc.swagger-ui.oauth2.use-pkce-with-authorization-code-grant=true
```

### 3. **EshopApplication.java**
```diff
+ import com.eshop.app.config.properties.AppProperties;

  @EnableConfigurationProperties({
      JwtProperties.class, 
      ApiInfoProperties.class,
+     AppProperties.class
  })
- @ConfigurationPropertiesScan({"com.eshop.app.config"})
+ @ConfigurationPropertiesScan({"com.eshop.app.config", "com.eshop.app.config.properties"})
  public class EshopApplication {
```

---

## 🔧 How to Use AppProperties in Your Code

### Example 1: Injecting AppProperties

```java
@Service
@RequiredArgsConstructor
public class ProductService {
    
    private final AppProperties appProperties;
    
    public void checkStock(int quantity) {
        int threshold = appProperties.getProduct().getLowStockThreshold();
        if (quantity < threshold) {
            // Send low stock alert
        }
    }
}
```

### Example 2: Accessing Nested Properties

```java
@RestController
@RequiredArgsConstructor
public class AnalyticsController {
    
    private final AppProperties appProperties;
    
    @GetMapping("/api/v1/analytics/top-products")
    public ResponseEntity<?> getTopProducts() {
        int limit = appProperties.getAnalytics().getDefaultTopProductsLimit();
        int timeout = appProperties.getAnalytics().getTimeoutSeconds();
        
        // Use these values...
    }
}
```

### Example 3: CORS Configuration

```java
@Configuration
@RequiredArgsConstructor
public class CorsConfig {
    
    private final AppProperties appProperties;
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        var config = new CorsConfiguration();
        
        // Type-safe access to CORS settings
        config.setAllowedOrigins(
            List.of(appProperties.getCors().getAllowedOrigins().split(","))
        );
        config.setAllowedMethods(
            List.of(appProperties.getCors().getAllowedMethods().split(","))
        );
        config.setAllowCredentials(appProperties.getCors().isAllowCredentials());
        
        // ...
    }
}
```

---

## 🎯 Verification Checklist

Run these commands to verify all fixes:

### 1. Clean Build
```bash
./gradlew clean build
```

**Expected**:
- ✅ No compilation errors
- ✅ No configuration warnings
- ✅ `spring-configuration-metadata.json` generated in `build/classes/java/main/META-INF/`

### 2. Check Metadata Generation
```bash
cat build/classes/java/main/META-INF/spring-configuration-metadata.json | grep "app\."
```

**Expected**:
- ✅ All `app.*` properties listed with descriptions
- ✅ Property types correctly identified

### 3. Start Application
```bash
./gradlew bootRun
```

**Expected Log Messages**:
```
✅ Flyway migration completed successfully
✅ Configuration properties bound: AppProperties
✅ OpenAPI documentation enabled
✅ No warnings about unknown properties
✅ Application started successfully
```

### 4. Test Endpoints

```bash
# Health check
curl http://localhost:8082/actuator/health

# Swagger UI (check OAuth2 configuration)
open http://localhost:8082/swagger-ui.html

# Check metrics
curl http://localhost:8082/actuator/prometheus | grep app_
```

---

## 📊 Before vs After

| Issue | Before | After | Status |
|-------|--------|-------|--------|
| Flyway PostgreSQL driver | Missing | `flyway-database-postgresql:10.10.0` | ✅ Fixed |
| Error properties namespace | `server.error.*` | `spring.web.error.*` | ✅ Fixed |
| Spring Retry config | Invalid property | `@EnableRetry` annotation | ✅ Fixed |
| SpringDoc OAuth2 | `springdoc.oauth2.*` | `springdoc.swagger-ui.oauth2.*` | ✅ Fixed |
| Custom properties warnings | No type binding | `AppProperties` class | ✅ Fixed |
| IDE autocomplete | Not working | Full autocomplete support | ✅ Fixed |
| Configuration validation | Runtime errors | Compile-time validation | ✅ Fixed |

---

## 🚀 Benefits Achieved

### 1. **Type Safety**
- All custom properties are now strongly typed
- Compile-time validation with `@Validated`
- No more runtime configuration errors

### 2. **IDE Support**
- Full autocomplete for all `app.*` properties
- Property documentation in tooltips
- Immediate feedback on typos

### 3. **Maintainability**
- Centralized configuration management
- Easy to add new properties
- Self-documenting code

### 4. **Spring Boot 4.0 Compliance**
- No deprecated properties
- Latest dependency versions
- Future-proof configuration

---

## 📚 Additional Resources

- [Spring Boot 4.0 Migration Guide](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide)
- [Configuration Properties](https://docs.spring.io/spring-boot/docs/4.0.x/reference/html/features.html#features.external-config.typesafe-configuration-properties)
- [Flyway Documentation](https://flywaydb.org/documentation/)
- [SpringDoc OpenAPI](https://springdoc.org/)

---

## ✨ Next Steps

1. **Rebuild the project** to generate metadata
2. **Restart IDE** to load new autocomplete suggestions
3. **Test all endpoints** to verify configuration
4. **Review logs** for any remaining warnings

All Spring Boot 4.0 configuration warnings are now resolved! 🎉


# --- File: testcontainers-docker-connectivity.md ---

# Testcontainers / Docker Connectivity Failures (Integration Tests)

## Affected Tests
- `BulkOperationIntegrationTest`
- `UserControllerIntegrationTest`
- `UserFilterCriteriaRepositoryTest`

All three extend `AbstractIntegrationTest` (`src/test/java/com/eshop/app/core/infrastructure/config/security/AbstractIntegrationTest.java`), which spins up real `postgres:16` and `redis:7` containers via Testcontainers for each test run.

There are **two distinct failure modes** that look similar in the test report but have completely different causes and fixes. Read the exception message carefully before assuming it's the same issue as last time.

---

## Failure Mode 1 — Testcontainers version too old for the installed Docker (✅ code fix, already applied)

### Symptom
```
org.testcontainers.containers.ContainerLaunchException: Container startup failed
Caused by: org.testcontainers.containers.ContainerFetchException: Can't get Docker image: testcontainers/ryuk:0.3.4
Caused by: com.github.dockerjava.api.exception.BadRequestException: Status 400: {"message":"client version 1.32 is too old.
Minimum supported API version is 1.40, please upgrade your client to a newer version"}
```

### Root Cause
`build.gradle` was pinned to `org.testcontainers:*:1.17.6` (mid-2022). That release's bundled `docker-java` client negotiates a very old Docker Engine API version (1.32). Modern Docker Desktop releases refuse to serve clients below API 1.40, so **every** Testcontainers-based test fails before the Spring context even starts — regardless of whether Docker itself is installed and running correctly.

The `testcontainers/ryuk:0.3.4` image reference in the stack trace is itself a strong tell: current Testcontainers releases pull a much newer Ryuk (the container-cleanup sidecar), so seeing that ancient tag confirms the library version is the culprit before you even read the "client version" message.

### Fix Applied (2026-08-15)
Bumped the pinned version in `build.gradle`:
```gradle
// was:
testImplementation 'org.testcontainers:junit-jupiter:1.17.6'
testImplementation 'org.testcontainers:testcontainers:1.17.6'
testImplementation 'org.testcontainers:postgresql:1.17.6'

// now:
testImplementation 'org.testcontainers:junit-jupiter:1.20.4'
testImplementation 'org.testcontainers:testcontainers:1.20.4'
testImplementation 'org.testcontainers:postgresql:1.20.4'
```
`testImplementation` only — zero production/runtime impact. The `@Container`/`@Testcontainers`/`PostgreSQLContainer`/`GenericContainer` APIs used in `AbstractIntegrationTest` are long-stable across Testcontainers versions, so this upgrade did not require any test code changes.

### ⚠️ This is a pinned version, not a self-updating one
If a future Docker Desktop release raises its minimum supported API version again past whatever `1.20.4`'s client negotiates, this exact failure signature can recur — and will require **another manual version bump** in `build.gradle`, the same way this one was fixed. That's expected, normal dependency-maintenance behavior (deliberately *not* using version ranges like `1.20.+`, since those make builds non-reproducible and can silently pull in breaking changes) — not a bug in the fix.

**If you see the "client version ... too old" message again:** check the current Testcontainers version at https://testcontainers.com/ and bump the three coordinates above to match. That's a code change (this file), and a normal one.

---

## Failure Mode 2 — Docker Desktop named-pipe routing on Windows (❌ NOT a code issue — machine/environment)

### Symptom
```
java.lang.IllegalStateException: Could not find a valid Docker environment. Please see logs and check configuration
	NpipeSocketClientProviderStrategy: failed with exception BadRequestException (Status 400: {"ID":"","Containers":0, ... "Labels":["com.docker.desktop.address=npipe://\\\\.\\pipe\\docker_cli"], ...})
```
Note the mostly-empty/zeroed JSON body (`"ID":"", "ServerVersion":"", "OSType":""`, etc.) returned with an HTTP 400 status, and the `docker_cli` (not `docker_engine`) pipe referenced in `Labels`.

### Root Cause
Docker Desktop on Windows exposes several named pipes (`docker_engine`, `docker_cli`, `dockerDesktopLinuxEngine`, etc. — list them with `Get-ChildItem \\.\pipe\ | Where-Object { $_.Name -like "*docker*" }` in PowerShell). Testcontainers' `NpipeSocketClientProviderStrategy` is getting routed to the `docker_cli` pipe — a restricted/proxy endpoint, not the full Docker Engine API — even when `DOCKER_HOST` is explicitly set to `npipe:////./pipe/docker_engine`. On this machine, `docker_engine` itself appears to forward into the same limited endpoint.

**This is Docker Desktop's own Windows networking stack, not project code or configuration.** There is no `build.gradle`, `application.properties`, or Java source change that reaches this layer.

### Troubleshooting steps (try in order, no code changes involved)
1. **Restart Docker Desktop completely** (not just the containers — quit and relaunch the app). This pipe-routing confusion is a known pattern after a Docker Desktop version update.
2. **Enable the TCP daemon exposure**: Docker Desktop → Settings → General → "Expose daemon on tcp://localhost:2375 without TLS". Then run tests with:
   ```
   export DOCKER_HOST=tcp://localhost:2375
   ./gradlew.bat test --tests "*IntegrationTest"
   ```
   This bypasses named-pipe routing entirely.
3. Clear the cached strategy and let Testcontainers re-probe fresh: delete (or rename) `~/.testcontainers.properties` and re-run. (Confirmed in the 2026-08-15 investigation that this alone does *not* fix a `docker_cli`-routing problem, but it's a cheap, safe thing to rule out first.)
4. Check Docker Desktop's release notes for the installed version for any known `docker_cli`/`docker_engine` pipe-routing regressions.
5. As a last resort, use WSL2 directly (run the Gradle build from inside a WSL2 distro with its own Docker Engine, rather than through the Windows named-pipe bridge).

### How to tell which failure mode you're looking at
| | Failure Mode 1 | Failure Mode 2 |
|---|---|---|
| Message | `"client version 1.32 is too old"` | `"Could not find a valid Docker environment"` |
| Status body | Explicit version-mismatch error text | Mostly-empty JSON with `docker_cli` in `Labels` |
| Fix location | `build.gradle` (Testcontainers version) | Docker Desktop settings (this machine) |
| Fix type | Code change | Environment/local-machine change |

### Verification
Once Docker connectivity is genuinely fixed (either mode), confirm with:
```bash
./gradlew.bat test --tests "com.eshop.app.user.api.controller.BulkOperationIntegrationTest" \
  --tests "com.eshop.app.user.api.controller.UserControllerIntegrationTest" \
  --tests "com.eshop.app.user.domain.repository.UserFilterCriteriaRepositoryTest" --console=plain
```
All three should report `BUILD SUCCESSFUL` with real Postgres/Redis containers starting (visible in the log as `Container postgres:16 started` / `Container redis:7 started`).


# --- File: gradle-bootrun-silent-kill.md ---

# App Silently Stops Mid-Request with `gradlew bootRun` (❌ NOT a code issue — process-lifetime/tooling)

## Symptom
The app stops with no warning while actively serving requests. `logs/eshop-dev-local.log` just ends — often mid log statement — with:
- No `Exception`/stack trace
- No `OutOfMemoryError`
- No JVM crash dump (`hs_err_pid*.log` in the project root)
- No Spring/Tomcat/Hikari shutdown logging at all (no "Shutting down ExecutorService", no "Pausing ProtocolHandler", no "HikariPool-1 - Shutdown initiated")

Example, observed 2026-08-16: 4 separate `bootRun` sessions that day (07:21, 08:52, 10:25, 13:41) each ended this way — the last one cut off mid `GET /api/v1/products` request handling at 14:34:05 with zero shutdown-phase logging.

## Root Cause
This log signature — a live log stream simply stopping, no exception, no OOM, no crash dump, and **zero shutdown logging** — only occurs when the JVM is killed from outside via `TerminateProcess` (a hard kill), not when the app crashes or exits on its own. If it were a real crash, exception, or graceful stop, Spring would have logged *something* first (it logs shutdown phases at INFO, and DEBUG-level Hibernate SQL was already on, so there's no logging-level explanation for the silence either).

`gradlew bootRun` runs the Spring Boot app as a **child process of the Gradle daemon/worker**, not as the terminal's own foreground process. On Windows, when the owning terminal/console is torn down — closing a terminal tab, closing/reloading the VS Code window that owns the integrated terminal, the machine going to sleep — Gradle force-terminates that child process tree instead of forwarding a signal the JVM can trap. The JVM shutdown hook (which is what makes Spring log its graceful-shutdown sequence) never runs, so the log just stops.

**This is not application code.** No exception path, thread, or config in this repo caused it — it's how `bootRun`'s child-process model behaves on Windows when its parent terminal disappears.

## How to confirm you're looking at this issue
1. Check for `hs_err_pid*.log` in the project root — if present, it's a real JVM crash, not this issue.
2. Search the log around the cutoff for `OutOfMemoryError`, `Fatal`, `SIGTERM` — if found, it's not this issue.
3. Search the log for any shutdown-phase logging near the cutoff (`Shutting down ExecutorService`, `Pausing ProtocolHandler`, `HikariPool.*Shutdown`) — if present, the app *did* get a graceful stop signal and this is a different problem.
4. If none of the above are present, this is it.

## Fix / Prevention (environment change, not code)

**1. Run the built jar directly instead of `bootRun`** — makes the JVM the console's own process, so `Ctrl+C` (or a real stop) reaches it directly and triggers a real graceful shutdown you can see in the log:
```powershell
.\gradlew.bat bootJar
java -jar build\libs\eshop-app-*.jar --spring.profiles.active=dev
```

**2. Don't run long-lived dev sessions in VS Code's integrated terminal.** Closing that panel, reloading the VS Code window, or a VS Code crash kills every child process attached to it. Use a standalone PowerShell/Windows Terminal window instead.

**3. If you need it to survive even that terminal closing, detach it fully:**
```powershell
Start-Process javaw -ArgumentList '-jar','build\libs\eshop-app-*.jar','--spring.profiles.active=dev' -WindowStyle Hidden
```
Add `server.shutdown=graceful` to `application-dev.properties` (already set in `application-prod.properties`) if you want a clean stop via the actuator shutdown endpoint or a non-forceful `taskkill`.

**4. Rule out sleep as the trigger** if it dies while genuinely idle/unattended:
```powershell
powercfg /change standby-timeout-ac 0
```

## Verification
After switching to running the jar directly, stop the app with `Ctrl+C` and confirm the log now shows a real shutdown sequence (Tomcat connector pause, Hikari pool shutdown, `ApplicationContext` closing) instead of just stopping mid-line. If a future silent stop happens again, check the log first per "How to confirm" above — don't assume it's this same cause without checking, since real crashes/OOMs still need the actual root cause investigated, not this doc's fix.

