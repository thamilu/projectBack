# Java and Spring Boot 4 Backend Standards

These standards apply to all backend changes in this repository. They complement the global enterprise standards in `G:\knowledge`, but this file is the project-local source of truth for Java, Gradle, and Spring Boot behavior.

## Runtime and Build Baseline

- Use Java 21 for development, compilation, tests, and runtime. The Gradle toolchain in `build.gradle` is authoritative.
- Do not use Java APIs introduced after Java 21.
- Keep the Gradle wrapper as the build entry point. Use `.\gradlew.bat` on Windows.
- Do not downgrade the Spring Boot Gradle plugin, Gradle wrapper, Java toolchain, or major Spring dependencies without a documented migration reason.
- Prefer Spring Boot managed dependency versions. Add explicit dependency versions only for libraries not managed by the Spring Boot BOM or when a compatibility issue is documented.

## Spring Boot 4.0.2 Rules

- Use Spring Boot 4 starter names and module layout.
- Prefer `spring-boot-starter-webmvc` for servlet MVC endpoints.
- Use WebFlux only for intentionally reactive integrations. Do not return Reactor types from normal MVC/JPA service flows unless the feature is designed end-to-end as reactive.
- Use `jakarta.*` APIs, not `javax.*`, for persistence, validation, servlet, and annotation APIs.
- Keep Spring Security 7 style configuration: define `SecurityFilterChain` beans and avoid deprecated adapter-based configuration.
- Prefer Spring Boot configuration properties with `@ConfigurationProperties` for grouped settings.
- Keep configuration externalized through `application-*.properties`, environment variables, or typed properties classes. Do not hardcode secrets, URLs, credentials, limits, or feature flags in code.
- Use Boot's Actuator, Micrometer, health indicators, and structured logging patterns already present in the project for observability.

## Dependency Rules

- Avoid pinning versions for Spring-managed libraries such as Spring Framework, Spring Security, Jackson, Micrometer, Hibernate, Tomcat, Logback, and validation unless there is a verified reason.
- Before adding a new dependency, check whether Java, Spring Framework, Spring Boot, or an existing project dependency already provides the capability.
- Treat Spring Boot 3 specific adapters as compatibility risks under Spring Boot 4. Verify before adding or upgrading them.
- Keep Lombok and MapStruct annotation processor ordering intact.
- Prefer maintained libraries with Java 21 and Jakarta compatibility.

## Architecture and Package Boundaries

- Keep the current layered structure: controller -> service -> repository.
- Controllers handle HTTP concerns, validation entry points, authentication context extraction, and response shaping.
- Services hold business rules, transactions, orchestration, and authorization-sensitive decisions.
- Repositories hold persistence access only. Do not put business rules in repository queries.
- DTOs define API input and output boundaries. Do not expose JPA entities directly from controllers.
- Mappers should stay in mapper packages and should use MapStruct where the existing module already uses it.
- New files are allowed when they are the correct design unit, such as DTOs, mappers, services, tests, migrations, or configuration classes. Do not force unrelated logic into existing files to avoid creating files.

## Persistence and Transactions

- Use Spring Data JPA repositories, specifications, projections, or explicit JPQL/native queries as appropriate.
- Use `@Transactional` at the service layer for multi-step write operations.
- Mark read-only service operations with `@Transactional(readOnly = true)` when they perform repository reads and benefit from transaction semantics.
- Prevent N+1 queries using fetch joins, projections, entity graphs, batching, or dedicated query methods.
- Use pagination for list endpoints. Prefer cursor pagination for high-volume or user-facing infinite-scroll flows.
- Add Flyway migrations for schema changes. Do not mutate existing applied migrations.
- Use parameterized queries only. Never concatenate user input into SQL or JPQL.

## Security

- Validate request DTOs with Jakarta Bean Validation.
- Use custom validators for cross-field or domain-specific constraints.
- Sanitize user-controlled HTML/text at boundaries where the data can later render in a client.
- Enforce authorization at the service or method-security layer for business-sensitive operations.
- Do not log tokens, passwords, OTPs, payment secrets, API keys, or raw PII.
- Keep CORS, CSRF, security headers, JWT validation, and OAuth2 resource server behavior explicit and test-covered when changed.
- Use Keycloak/OIDC for identity and role claims. Do not add custom password-auth flows unless explicitly required.

## API and Error Handling

- Keep REST endpoints versioned under the existing API version convention.
- Return stable DTOs and avoid leaking internal exception messages.
- Use the project's standardized error response shape consistently.
- Include correlation/request IDs in logs and error responses where the existing infrastructure supports it.
- Use correct HTTP status codes and avoid always returning `200` for failed operations.

## Testing Requirements

- Add or update tests for every behavior change.
- Prefer focused unit tests for service logic and validators.
- Use Spring Boot test slices or integration tests for controllers, security behavior, repositories, and configuration wiring.
- Use Testcontainers or project-approved integration infrastructure for database behavior that cannot be proven with unit tests.
- Run at least `.\gradlew.bat test` before considering backend changes complete when the change touches Java code.
- For dependency, configuration, or migration changes, also run the narrowest relevant Gradle task that proves startup or schema compatibility.

## Code Quality

- Keep methods small enough to read without hiding meaningful flow behind unnecessary abstractions.
- Prefer constructor injection.
- Avoid static mutable state.
- Avoid catching broad exceptions unless translating them at a boundary with useful context.
- Use `Optional` for absent return values from service/repository helpers where it improves clarity, not for DTO fields or entity fields.
- Use `BigDecimal` for money and quantities that require decimal precision.
- Use `Instant`, `OffsetDateTime`, or `LocalDate` intentionally. Do not use legacy `Date`/`Calendar` in new code.
- Keep comments reserved for non-obvious decisions, invariants, or integration constraints.

## Change Checklist

Before finishing a backend change:

1. Confirm the code compiles with Java 21.
2. Confirm imports use `jakarta.*` where applicable.
3. Confirm new dependencies are necessary and compatible with Spring Boot 4.
4. Confirm controllers do not expose entities directly.
5. Confirm validation, authorization, and error behavior are covered.
6. Confirm persistence changes avoid N+1 and include migrations when schema changes.
7. Confirm tests were added or updated and the relevant Gradle task was run.
