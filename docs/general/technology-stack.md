# 🛠️ Technology Stack — EShop Application

This document describes the primary technologies, libraries, and conventions used by the EShop backend and how to run/configure the service locally.

## Overview
- Language: Java 21
- Framework: Spring Boot 4 / Spring Framework 7
- Build: Gradle

## Core Backend
- Java 21 (LTS)
- Spring Boot 4 (Spring Framework 7)
- Spring Web MVC (REST controllers)
- Spring Data JPA (Hibernate) — data access layer
- Flyway — database migrations (db/migration)

## Caching
- Caffeine — primary in-memory cache (configured via `CacheConfig`)
- Cache names centralized in `CacheConfig` to avoid typos (e.g. `CATEGORIES_CACHE`, `CATEGORY_CACHE`, `CATEGORY_LIST_CACHE`).

## Resilience & Reliability
- Spring Retry / Resilience4j patterns used for transient failures
- Retry policies are used selectively; business exceptions are not wrapped so they propagate to the global exception handler.

## Security & Identity
- Spring Security (OAuth2 Resource Server) for JWT validation
- Keycloak used as identity provider (local docker-compose for dev)
- CSRF is disabled for stateless REST endpoints (see `EnhancedSecurityConfig`).

## Persistence
- PostgreSQL (recommended 14+) — configured via application properties
- HikariCP as connection pool

## API & Documentation
- OpenAPI 3 / Springdoc (Swagger UI) for interactive API docs
- ProblemDetail-based global exception handling for consistent error responses

## Observability
- Spring Boot Actuator endpoints enabled
- Prometheus-compatible metrics export recommended (Actuator + Prometheus)
- Logback for structured logging

## Testing & Quality
- JUnit 5 for unit/integration tests
- Mockito / Spring Test utilities for mocking and slice tests

## Dev & Deployment Tooling
- Gradle wrapper (`gradlew` / `gradlew.bat`) for builds
- Docker & Docker Compose for local environment (Keycloak, DB)
- Recommended CI: any Gradle-capable runner (GitHub Actions, Azure Pipelines, etc.)

## Notable Libraries and Uses
- Lombok: reduces boilerplate for DTOs/Entities
- MapStruct: compile-time DTO mapping where appropriate
- Spring Retry / Resilience4j: transparent retry and circuit-breaker strategies

## Important Files & Locations
- Application config: [src/main/resources/application.properties](src/main/resources/application.properties)
- Security config: [src/main/java/com/eshop/app/config/EnhancedSecurityConfig.java](src/main/java/com/eshop/app/config/EnhancedSecurityConfig.java)
- Cache config and names: [src/main/java/com/eshop/app/config/CacheConfig.java](src/main/java/com/eshop/app/config/CacheConfig.java)
- Global exception handling: [src/main/java/com/eshop/app/common/exception/ProblemDetailExceptionHandler.java](src/main/java/com/eshop/app/common/exception/ProblemDetailExceptionHandler.java)
- DB migrations: [src/main/resources/db/migration](src/main/resources/db/migration)

## Running Locally (quick)
1. Start dependent services (Postgres, Keycloak) via Docker Compose if needed:

```powershell
docker-compose -f docker-compose.keycloak.yml up -d
docker-compose up -d postgres
```

2. Build (skip tests for quicker feedback):

```powershell
./gradlew.bat clean build -x test
```

3. Run with `dev` profile on alternate port if 8082 is occupied:

```powershell
./gradlew.bat bootRun -x test --args="--spring.profiles.active=dev --server.port=8083"
```

## Environment & Common Properties
- `spring.datasource.url`, `spring.datasource.username`, `spring.datasource.password` — DB connection
- `spring.profiles.active` — profile selection (`dev`, `prod`, `test`)
- `spring.security.oauth2.resourceserver.jwt.*` — JWT validation settings

## Conventions & Best Practices
- Centralize cache names in `CacheConfig` to avoid IllegalArgumentException caused by missing caches.
- Throw domain/business exceptions (e.g. `DuplicateResourceException`) directly so the ProblemDetail handler can map them to 409/404.
- Use DTOs for public APIs; map entities via `MapStruct`.

## Quick Troubleshooting
- If you see `SpelEvaluationException: 'content' cannot be found` — ensure `PageResponse` has `getContent()` accessor (compatibility with templates/SpEL).
- If app fails to start due to port conflict: change `--server.port` in bootRun args or stop the conflicting process.

---

If you'd like, I can expand this into a CONTRIBUTING section, add exact dependency versions, or generate a short README with run/debug recipes for Windows and Linux.
