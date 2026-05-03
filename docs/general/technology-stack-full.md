# EShop — Full Technology Stack

This document lists the complete technology stack used in the EShop project, including the primary libraries and versions (taken from `build.gradle`), runtime services, configuration pointers, and quick run/debug commands.

---

## Project summary
- Language: Java 21 (toolchain configured in Gradle)
- Framework: Spring Boot 4 (Spring Framework 7)
- Build: Gradle (wrapper provided)

## Key build plugins
- `org.springframework.boot` plugin — 4.0.1
- `io.spring.dependency-management` plugin — 1.1.7

## Primary runtime libraries (selected, with versions)
- Spring Boot starters: WebMVC, Data JPA, Security, Actuator, Cache, Validation (via Spring Boot 4)
- Caffeine: `com.github.ben-manes.caffeine:caffeine:3.1.8`
- Springdoc OpenAPI (Swagger UI): `org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.0`
- JJWT (JWT helpers): `io.jsonwebtoken:jjwt-api:0.12.3` (+ `jjwt-impl` and `jjwt-jackson` at runtime)
- Flyway (DB migrations): `org.flywaydb:flyway-core:10.10.0` (+ PostgreSQL db module)
- Resilience4j: `io.github.resilience4j:resilience4j-spring-boot3:2.2.0` and related modules (circuitbreaker, retry, ratelimiter, bulkhead, timelimiter)
- Micrometer: `io.micrometer:micrometer-core:1.16.0` and Prometheus registry `1.16.0`
- MapStruct: `org.mapstruct:mapstruct:1.6.3` (+ processor)
- Lombok: `org.projectlombok:lombok:1.18.42` and `lombok-mapstruct-binding:0.2.0`
- Jackson Databind: `com.fasterxml.jackson.core:jackson-databind:2.16.0`
- PostgreSQL JDBC Driver: `org.postgresql:postgresql` (runtime)
- Stripe SDK: `com.stripe:stripe-java:24.18.0`
- PayPal Checkout SDK: `com.paypal.sdk:checkout-sdk:2.0.0`
- Razorpay SDK: `com.razorpay:razorpay-java:1.4.6`
- Thumbnailator (image processing): `net.coobird:thumbnailator:0.4.19`
- Cloudinary SDK: `com.cloudinary:cloudinary-http44:1.36.0`
- Apache Commons Lang: `org.apache.commons:commons-lang3:3.14.0`
- Apache POI (Excel): `org.apache.poi:poi-ooxml:5.2.5`
- iText (PDF): `com.itextpdf:itext7-core:8.0.2`
- ShedLock (scheduled locking): `net.javacrumbs.shedlock:shedlock-spring:5.10.0` + JDBC provider
- JSR-354 Money API + Moneta: `javax.money:money-api:1.1`, `org.javamoney:moneta:1.4.2`
- Jsoup (HTML sanitization): `org.jsoup:jsoup:1.18.1`

## Observability & Tracing
- Spring Boot Actuator
- Micrometer Prometheus registry
- Micrometer tracing bridge + Zipkin reporter (for distributed tracing): `micrometer-tracing-bridge-brave`, `zipkin-reporter-brave`

## Caching
- Caffeine as primary in-memory cache (configured in `CacheConfig`)
- Cache names centralized in code (`CacheConfig`) to avoid missing-cache errors

## Resilience & Reliability
- Resilience patterns via Resilience4j and Spring Retry
- Retry used for transient operations; business exceptions should not be wrapped so they propagate to global handlers

## Security & Identity
- Spring Security (OAuth2 Resource Server) for JWT validation
- Keycloak (containerized) used as identity provider for dev (Docker Compose files included)
- CSRF disabled for stateless REST endpoints in `EnhancedSecurityConfig`

## Persistence
- Primary DB: PostgreSQL (production); H2 used in tests
- Flyway for DB migrations (scripts in `src/main/resources/db/migration`)
- HikariCP as the default connection pool (via Spring Boot)

## Testing
- JUnit 5 + Spring Boot test starter
- Spring Security test helpers
- H2 runtime for faster in-memory tests

## Dev tooling & infra
- Gradle wrapper (`gradlew`, `gradlew.bat`) — use for builds and running app
- Docker & Docker Compose (compose files: `docker-compose.keycloak.yml`, `keycloak-docker-compose.yml`, `docker-compose.prod.yml`)
- Git for version control

## Important project files / paths
- Application config: `src/main/resources/application.properties` and profile-specific variants
- Cache config: `src/main/java/com/eshop/app/config/CacheConfig.java`
- Security config: `src/main/java/com/eshop/app/config/EnhancedSecurityConfig.java`
- Global exception handler: `src/main/java/com/eshop/app/common/exception/ProblemDetailExceptionHandler.java`
- DB migrations: `src/main/resources/db/migration`
- DTOs / Responses: `src/main/java/com/eshop/app/dto` (includes `PageResponse` compatibility getters)

## Defaults & local run commands
1. Start infra (Keycloak, Postgres) via compose if needed:

```powershell
# start Keycloak (example)
docker-compose -f docker-compose.keycloak.yml up -d
# start Postgres (if you have a compose target)
docker-compose up -d postgres
```

2. Build the project (skip tests for faster iteration):

```powershell
./gradlew.bat clean build -x test
```

3. Run the app (dev profile). If port 8082 is in use, override port:

```powershell
./gradlew.bat bootRun -x test --args="--spring.profiles.active=dev --server.port=8083"
```

## Environment variables commonly used
- `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`
- `SPRING_PROFILES_ACTIVE`
- `SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK-SET-URI` or `spring.security.oauth2.resourceserver.jwt.*` props
- Payment provider keys: `STRIPE_API_KEY`, `PAYPAL_CLIENT_ID`, `PAYPAL_SECRET`, `RAZORPAY_KEY` (store securely)

## Troubleshooting tips
- SpEL `content` missing on responses: ensure `PageResponse` exposes `getContent()` (compatibility helper added in DTO).
- Missing cache name error: confirm cache names exist in `CacheConfig` and that your `@Cacheable`/`@CachePut` annotations use constants.
- Port already in use: change `--server.port` or stop the occupying process.

## How to get a complete dependency BOM
Run the Gradle dependency report for the runtime classpath:

```powershell
./gradlew.bat dependencies --configuration runtimeClasspath > deps.txt
```

## Next recommendations (optional)
- Add `README.md` with explicit Windows and Linux run/debug recipes.
- Add `CONTRIBUTING.md` describing local dev flow and how to run tests and migrations.
- Add a `docker-compose.dev.yml` that brings up Postgres + Keycloak + the app (optional automated local dev stack).

---

If you want, I can now:
- create a concise `README.md` with the exact commands tailored for Windows (I can do that next), or
- add a `CONTRIBUTING.md` describing how to set up Keycloak and Postgres locally.

Tell me which one to do next.
