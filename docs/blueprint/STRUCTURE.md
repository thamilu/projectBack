# Enterprise Architectural Blueprint: Definitive Technical Map (Backend)

This document is the **Ultimate Source of Truth** for the E-Shop Enterprise Java Backend workspace. It reflects the actual filesystem state after the Domain-Driven Modular Monolith hardening (v2.0, 2026-05-09).

> [!IMPORTANT]
> **Admin is an Actor, not a Domain.** Admin endpoints live inside their respective domain controllers under `/admin/` path segments. A standalone `admin` module would become a "God Module" tightly coupled to every other domain.

```text
eshop_back/
├── ⚙️ [Root Configuration & Infrastructure Files]
│   ├── .env                                    # Environment variables
│   ├── .env.example                            # Environment template
│   ├── .gitignore                              # Git exclusion rules
│   ├── build.gradle                            # Gradle build & Spring BOM dependencies
│   ├── settings.gradle                         # Gradle project name
│   ├── gradlew / gradlew.bat                   # Gradle wrapper (ALWAYS use this, never global gradle)
│   ├── docker-compose.yml                      # Local infrastructure (Postgres, Redis, Keycloak)
│   ├── docker-compose-dev.yml                  # Development profile overrides
│   ├── docker-compose.prod.yml                 # Production infrastructure stack
│   ├── Dockerfile                              # Application containerization
│   └── README.md                               # Project setup guide
│
├── 👻 [Hidden & Tooling Directories]
│   ├── .gradle/                                # Gradle caches & daemon
│   ├── .idea/                                  # IntelliJ IDE settings
│   ├── .vscode/                                # VS Code workspace settings
│   └── build/                                  # BUILD ARTIFACTS (Gitignored)
│
├── 🗄️ docker/                                  # CONTAINER CONFIGURATIONS
│   ├── keycloak/                               # Custom Keycloak realm & container setup
│   └── postgres/                               # Postgres initialization scripts
│
├── 🛡️ keycloak-themes/                         # IDENTITY PROVIDER THEMING
│   └── eshop/login/resources/css/              # Premium E-Shop Keycloak UI Theme
│
├── 📝 logs/                                    # APPLICATION LOGS (Gitignored in prod)
│
├── 🌐 nginx/                                   # REVERSE PROXY & SSL TERMINATION
│   ├── logs/                                   # Proxy access/error logs
│   └── ssl/                                    # TLS certificate storage
│
├── 🛠️ scripts/                                   # DATA UTILITIES & ETL PIPELINES
│   │   RULE: Registered Gradle source set ('scripts').
│   │   Linked to project dependencies for safe standalone execution.
│   └── data-utils/                             # CSV/Excel/JSON transformation tools
│       ├── CsvToJsonConverter.java             # Postal data generator
│       └── ExcelToCsvTalukMerger.java          # Taluk reconciliation utility
│
├── 📁 docs/                                    # ENTERPRISE KNOWLEDGE REGISTRY
│   ├── blueprint/                              # ← This file. Architecture & Structure.
│   │   └── STRUCTURE.md
│   ├── architecture/                           # High-level system design diagrams
│   ├── database/                               # ERDs and migration notes
│   ├── guides/                                 # Developer onboarding & standards
│   ├── reference/                              # External API specifications
│   └── ...                                     # (authentication, deployment, setup, etc.)
│
├── 🧪 src/test/java/com/eshop/app/            # UNIT & INTEGRATION TEST SUITES
│   ├── config/                                 # Test Spring configuration
│   ├── entity/                                 # Entity unit tests
│   ├── security/                               # Security unit tests
│   ├── service/                                # Service unit tests
│   │   ├── analytics/
│   │   └── impl/
│   ├── validation/                             # Validation unit tests
│   └── web/                                    # @WebMvcTest slice tests
│
├── 📂 src/main/resources/                      # APPLICATION RESOURCES
│   ├── application.yml                         # Spring Boot main configuration
│   ├── application-dev.yml                     # Development profile
│   ├── application-prod.yml                    # Production profile
│   ├── db/migration/                           # FLYWAY MIGRATIONS (V1__*.sql, ...)
│   ├── seed/                                   # JSON seed data files
│   ├── examples/                               # API example payloads
│   ├── static/                                 # Static resources
│   └── templates/                              # Email/report templates
│
└── ☕ src/main/java/com/eshop/app/             # ══ SPRING BOOT 4 ENTERPRISE BACKEND ══
    │
    ├── 🧩 core/                                # ENTERPRISE CROSS-CUTTING CONCERNS
    │   │   ⚠️ RULE: ZERO business logic here.
    │   │   These are pure infrastructure components shared across ALL domains.
    │   │
    │   ├── 🕵️ audit/                           # AUDIT TRAIL
    │   │   │   Mandatory for all ADMIN, FINANCIAL & SELLER-modifying operations.
    │   │   │   AOP-driven: annotate a method → audit record saved automatically.
    │   │   ├── Auditable.java                  # @Auditable method annotation
    │   │   └── AuditLoggingAspect.java         # Async AOP interceptor → AuditLog persistence
    │   │
    │   ├── 💾 cache/                           # ENTERPRISE CACHE ABSTRACTION
    │   │   └── ResilientRedisCacheService.java # Circuit-breaker + retry Redis wrapper
    │   │                                       # All Redis access MUST go through this class
    │   │
    │   ├── 📡 events/                          # DOMAIN EVENT ARCHITECTURE
    │   │   │   Decouples side-effects (notifications, indexing) from business transactions.
    │   │   │   Listeners run async AFTER transaction commit — never block the main flow.
    │   │   ├── domain/                         # Immutable event POJOs (published by services)
    │   │   │   ├── LowStockEvent.java          # Fires when stock < threshold
    │   │   │   ├── ProductCreatedEvent.java    # Fires on product creation
    │   │   │   └── StockChangedEvent.java      # Fires on any stock delta (with full audit trail)
    │   │   ├── listeners/                      # Async, post-commit event consumers
    │   │   │   └── ProductEventListener.java   # Orchestrates: Indexing, Notifications, Auditing
    │   │   └── publishers/                     # (Reserved) Outbound event publishers (Kafka/RabbitMQ)
    │   │
    │   ├── ⚙️ jobs/                            # BACKGROUND JOBS (Domain-segregated)
    │   │   ├── CacheWarmingJob.java            # Prevents cache stampede — runs on schedule
    │   │   └── product/                        # (Reserved) Product-specific background jobs
    │   │
    │   ├── 🔄 outbox/                          # IDEMPOTENCY & TRANSACTIONAL OUTBOX
    │   │   └── IdempotencyInterceptor.java     # X-Idempotency-Key enforcement at HTTP layer
    │   │                                       # Prevents duplicate POSTs for payments/orders
    │   │
    │   ├── 📊 observability/                   # METRICS, TRACING & STRUCTURED LOGGING
    │   │   └── MetricsService.java             # Micrometer facade with standardized naming
    │   │                                       # Prefix: eshop.{domain}.{metric}
    │   │
    │   └── 🔐 permissions/                     # CENTRALIZED ACCESS POLICY
    │       └── AccessPolicy.java               # @accessPolicy Spring bean
    │                                           # Use in @PreAuthorize("@accessPolicy.isAdmin()")
    │                                           # Replaces scattered, untestable role checks
    │
    ├── 🌐 config/                              # SPRING CONTROL PLANE
    │   │   # All configurations must be Environment-based.
    │   │   # No hardcoded values. Use @ConfigurationProperties.
    │   ├── properties/                         # Typed @ConfigurationProperties
    │   │   ├── AppProperties.java
    │   │   ├── SecurityProperties.java
    │   │   ├── SeedProperties.java
    │   │   └── SwaggerProperties.java
    │   ├── resilience/                         # Resilience4j config
    │   │   └── Resilience4jConfig.java         # Circuit Breakers, Retry, Bulkheads
    │   ├── security/                           # Spring Security 7 FilterChain
    │   │   ├── SecurityConfig.java
    │   │   ├── DevJwtDecoderConfig.java
    │   │   └── MultiTenantJwtConfiguration.java
    │   └── startup/                            # Startup validators & health checks
    │       └── CredentialValidator.java
    │   # Also contains: CacheConfig, CorsConfig, WebConfig, SwaggerConfig,
    │   # VirtualThreadConfig, SchedulingConfig, JacksonConfig, etc.
    │
    ├── 🧱 controller/                          # PRESENTATION LAYER (HTTP Entry Points)
    │   │   RULE: HTTP routing, input validation, and auth context ONLY.
    │   │   Delegates immediately to the service layer. Zero business logic.
    │   │   Admin endpoints live here under /admin/ paths (Admin = Actor, not Domain).
    │   ├── AdminApprovalController.java        # Admin: seller approvals (/api/v1/admin/*)
    │   ├── AdminCategoryController.java        # Admin: category management
    │   ├── BrandController.java                # Public: brand catalog
    │   ├── CartController.java / ShoppingCartController.java
    │   ├── CategoryController.java
    │   ├── CouponController.java
    │   ├── DashboardController.java            # Customer dashboard telemetry
    │   ├── DeliveryController.java
    │   ├── HomeController.java                 # Public homepage data
    │   ├── MeController.java                   # Authenticated user profile
    │   ├── OrderController.java
    │   ├── PaymentController.java / PaymentWebhookController.java
    │   ├── ProductController.java / ProductImageController.java / ProductReviewController.java
    │   ├── SellerController.java / SellerCategoryController.java / SellerStoreController.java
    │   ├── ShippingController.java
    │   ├── UserController.java
    │   ├── WishlistController.java
    │   ├── SessionController.java
    │   └── auth/                               # Auth-specific endpoints
    │       ├── KeycloakAuthController.java
    │       └── KeycloakAdminController.java
    │
    ├── ⚙️ service/                             # BUSINESS LOGIC LAYER
    │   │   RULE: All business rules, orchestration & transactions live here.
    │   │   @Transactional declared at THIS layer only.
    │   │   Read-only operations MUST use @Transactional(readOnly = true).
    │   ├── SearchIndexService.java (interface)# Search engine synchronization
    │   ├── NotificationService.java (interface)# Event-driven alerts (Admin/User)
    │   ├── ReportService.java (interface)      # Analytics & compliance logging
    │   ├── ProductService.java (interface)     # Product catalog logic
    │   ├── OrderService.java (interface)       # Order lifecycle & state
    │   ├── SellerProfileService.java (interface)
    │   ├── UserService.java, CartService.java, WishlistService.java ...
    │   ├── AdminDashboardService.java          # Admin cross-domain orchestration
    │   ├── EmailService.java                   # Notification sending
    │   ├── SecureFileUploadService.java        # File upload & validation
    │   ├── UserAuditService.java               # Audit record creation
    │   ├── analytics/                          # Analytics orchestration
    │   │   ├── AdminAggregationService.java
    │   │   ├── AdminAnalyticsService.java
    │   │   ├── SellerAggregationService.java
    │   │   └── SellerAnalyticsService.java
    │   ├── auth/                               # Keycloak & identity coordination
    │   │   ├── KeycloakAdminService.java
    │   │   └── KeycloakAuthService.java
    │   ├── cache/                              # (Legacy path → migrate to core/cache over time)
    │   │   └── ResilientRedisCacheService.java # ⚠️ Superseded by core/cache/
    │   └── impl/                              # Concrete implementations
    │       ├── ProductServiceImpl.java
    │       ├── OrderServiceImpl.java
    │       ├── SellerProfileServiceImpl.java
    │       └── ... (all other *ServiceImpl.java)
    │
    ├── 🗄️ repository/                          # DATA ACCESS LAYER
    │   │   RULE: Pure persistence ONLY. No business logic or DTO construction.
    │   │   Prefer projections over full entity returns for read operations.
    │   ├── ProductRepository.java
    │   ├── OrderRepository.java
    │   ├── UserRepository.java
    │   ├── AuditLogRepository.java             # Admin audit trail persistence
    │   ├── StockMovementRepository.java        # Inventory history & reconciliation
    │   ├── SellerProfileRepository.java
    │   ├── ... (all other *Repository.java)
    │   ├── projection/                         # DTO PROJECTIONS (N+1 prevention)
    │   │   ├── ProductDetailProjection.java
    │   │   ├── ProductSummaryProjection.java
    │   │   └── PriceStatsProjection.java
    │   └── analytics/                          # Analytics-specific query interfaces
    │       └── AnalyticsOrderRepository.java
    │
    ├── 🏗️ entity/                              # JPA DOMAIN MODELS
    │   │   RULE: Entities NEVER cross the service boundary. Use DTOs + MapStruct.
    │   ├── BaseEntity.java                     # Shared audit fields (createdAt, updatedAt)
    │   ├── AuditLog.java                       # Audit trail persistence model
    │   ├── StockMovement.java                  # Inventory change audit model
    │   ├── Product.java, Order.java, User.java, SellerProfile.java ...
    │   └── enums/                              # Entity state enumerations (ProductStatus, etc.)
    │
    ├── 🛡️ dto/                                 # DATA TRANSFER OBJECTS
    │   │   RULE: All API I/O uses DTOs. Entities never cross this boundary.
    │   ├── request/                            # Incoming payloads (Jakarta Bean Validation)
    │   │   ├── ProductCreateRequest.java, OrderCreateRequest.java ...
    │   │   └── SellerRegisterRequest.java, etc.
    │   ├── response/                           # Outgoing payloads (no sensitive fields)
    │   │   ├── ProductResponse.java, OrderResponse.java ...
    │   │   ├── SellerDashboardResponse.java, AdminDashboardResponse.java ...
    │   │   ├── PageResponse.java               # Standardized paginated wrapper
    │   │   └── ApiError.java / ErrorResponse.java
    │   ├── analytics/                          # Analytics-specific DTOs
    │   │   ├── AdminStatistics.java
    │   │   └── SellerStatistics.java
    │   ├── attributes/                         # Category attribute DTOs
    │   │   ├── ElectronicsAttributes.java
    │   │   ├── ClothingAttributes.java
    │   │   └── ...
    │   ├── auth/                               # Auth-specific DTOs
    │   │   ├── LoginRequest.java, RegisterRequest.java
    │   │   └── TokenResponse.java, UserInfoResponse.java
    │   ├── error/                              # Global error DTOs
    │   │   └── ApiError.java                  # Standard error response envelope
    │   └── common/                             # Shared DTO fragments
    │       └── StandardApiResponse.java
    │
    ├── 🔐 auth/                                # AUTHENTICATION & RBAC INTERNALS
    │   ├── aspect/                             # Security-specific AOP components
    │   ├── constants/                          # Auth-specific constants
    │   ├── dto/response/                       # Auth response DTOs
    │   ├── exception/                          # Auth-specific exceptions
    │   ├── service/                            # Auth service interfaces
    │   └── validator/                          # Custom token & role validators
    │
    ├── ⚠️ exception/                           # GLOBAL ERROR HANDLING
    │   └── handler/                            # @ControllerAdvice — uniform ApiError format
    │                                           # RULE: No raw errors ever exposed to clients
    │
    ├── 🔀 mapper/                              # OBJECT MAPPING (MapStruct)
    │   │   RULE: Entity ↔ DTO conversion ONLY. Zero business logic in mappers.
    │   ├── BrandMapper.java, ProductMapper.java, UserMapper.java ...
    │   ├── SellerMapper.java, StoreMapper.java, WishlistMapper.java
    │   ├── EntityMapper.java                   # Common mapping interface
    │   └── config/                             # MapStruct shared configurations
    │       ├── BaseMapper.java
    │       ├── CommonMappingConfig.java
    │       ├── MapperConfiguration.java
    │       └── ReferenceMapper.java
    │
    ├── 🚦 validation/                          # JAKARTA BEAN VALIDATION
    │   │   Custom constraint annotations for enterprise-grade input safety.
    │   ├── @NoHtml / NoHtmlValidator.java      # Prevents XSS via HTML injection
    │   ├── @SafeText / SafeTextValidator.java  # General input sanitization
    │   ├── @PasswordMatches / Validator        # Cross-field password confirmation
    │   ├── @ValidPriceDiscount / Validator     # Business rule: discount < base price
    │   ├── @RateLimited                        # Method-level rate limiting annotation
    │   └── ValidationGroups.java              # Jakarta Validation group markers
    │
    ├── 🕵️ aspect/                              # AOP CROSS-CUTTING ASPECTS (Legacy)
    │   │   ⚠️ NOTE: Audit aspects migrated to core/audit/. Remaining aspects stay here.
    │   ├── Auditable.java                      # ⚠️ SUPERSEDED — use core/audit/Auditable.java
    │   ├── AuditLoggingAspect.java             # ⚠️ SUPERSEDED — use core/audit/AuditLoggingAspect.java
    │   ├── PageableValidationAspect.java        # Validates Pageable params before repository calls
    │   ├── PaginationLimitAspect.java           # Enforces max page size globally
    │   └── RateLimitingAspect.java             # Rate limiting enforcement via Redis
    │
    ├── 🕵️ filter/                              # SERVLET FILTERS (Middleware Chain)
    │   ├── RequestLoggingFilter.java           # Structured request/response logging (with MDC)
    │   └── CorrelationIdFilter.java            # X-Correlation-ID propagation
    │
    ├── 🛂 interceptor/                         # SPRING INTERCEPTORS (Legacy)
    │   └── IdempotencyInterceptor.java         # ⚠️ SUPERSEDED — use core/outbox/IdempotencyInterceptor.java
    │
    ├── 📡 event/                               # DOMAIN EVENTS (Legacy)
    │   ├── LowStockEvent.java                  # ⚠️ SUPERSEDED — use core/events/domain/
    │   ├── ProductCreatedEvent.java            # ⚠️ SUPERSEDED — use core/events/domain/
    │   ├── StockChangedEvent.java              # ⚠️ SUPERSEDED — use core/events/domain/
    │   └── ProductEventListener.java          # ⚠️ SUPERSEDED — use core/events/listeners/
    │
    ├── 🔒 security/                            # SPRING SECURITY COMPONENTS
    │   ├── PrincipalDetails.java               # Custom UserDetails implementation
    │   ├── CurrentUser.java                    # @CurrentUser parameter annotation
    │   ├── JwtAuthenticationEntryPoint.java    # 401 Unauthorized entry point
    │   ├── AttributeEncryptor.java             # JPA attribute encryption (AES-256)
    │   ├── ProductSecurityService.java         # Product-level ownership checks
    │   └── RateLimited.java                    # Rate limiting annotation
    │
    ├── 🔎 specification/                       # JPA SPECIFICATION PATTERN
    │   └── ProductSpecificationBuilder.java    # Dynamic filter specs for product search
    │
    ├── 🗂️ storage/                             # FILE & MEDIA STORAGE
    │   ├── ImageStorageService.java            # Storage interface
    │   ├── ImageStorageFactory.java            # Factory: LocalStorage vs R2Storage
    │   ├── R2StorageService.java               # Cloudflare R2 implementation
    │   └── ImageUploadResult.java             # Upload result DTO
    │
    ├── 🎯 strategy/                            # STRATEGY PATTERN
    │   ├── SellerRegistrationValidator.java    # Strategy interface
    │   └── impl/                              # Concrete validator strategies
    │       ├── BusinessSellerValidator.java
    │       ├── FarmerActivityValidator.java
    │       ├── GlobalSellerValidator.java
    │       └── IndividualSellerValidator.java
    │
    ├── ⚙️ processor/                           # SELLER MODULE PROCESSORS
    │   ├── SellerModuleProcessor.java          # Processor interface
    │   └── impl/                              # Concrete processors
    │       ├── BankProcessor.java
    │       ├── BusinessDetailsProcessor.java
    │       ├── FarmerProcessor.java
    │       └── KycProcessor.java
    │
    ├── 🏥 health/                              # CUSTOM HEALTH INDICATORS
    │   └── HikariConnectionPoolHealthIndicator.java
    │
    ├── 📅 scheduler/                           # SCHEDULERS (Legacy)
    │   └── CacheWarmingScheduler.java          # ⚠️ SUPERSEDED — use core/jobs/CacheWarmingJob.java
    │
    ├── 🔢 constants/                           # IMMUTABLE APPLICATION CONSTANTS
    │   ├── ApiConstants.java                   # Cache names, limits, API constants
    │   └── ApiVersion.java                     # API version string constants
    │
    ├── 🧭 common/                              # SHARED BASE COMPONENTS
    │   ├── controller/BaseController.java      # Shared controller helpers
    │   ├── exception/GlobalExceptionHandler.java
    │   ├── filter/CorrelationIdFilter.java
    │   └── util/ETagGenerator.java
    │
    ├── 🌱 seed/                                # DATABASE SEEDING (Dev/Staging Only)
    │   ├── core/                               # Orchestration framework (BaseSeeder, SeedOrchestrator)
    │   ├── seeders/                            # Domain-specific data seeders
    │   │   ├── UserSeeder.java, ProductSeeder.java, CategorySeeder.java ...
    │   │   └── BrandSeeder.java, StoreSeeder.java, TagSeeder.java, CartSeeder.java
    │   ├── provider/                           # JSON-based data providers
    │   │   └── JsonFile*DataProvider.java (Brand, Category, Product, Store, Tag, User)
    │   ├── model/                              # Seed data models
    │   ├── service/                            # Seeding support services
    │   ├── exception/                          # Seeding-specific exceptions
    │   ├── validation/                         # Seed config validators
    │   └── config/                             # DataSeederConfig (conditional bean setup)
    │
    ├── 💎 util/                                # PURE STATELESS UTILITIES
    │   │   STRICT RULE: Pure functions only. Zero Spring beans, DB access, or HTTP context.
    │   ├── SecurityUtils.java                  # SecurityContext accessor helpers
    │   ├── PaginationUtils.java                # Pageable construction helpers
    │   ├── DateTimeUtils.java                  # Date/time formatting utilities
    │   ├── SlugUtils.java                      # URL slug generation
    │   ├── SearchUtils.java                    # Search query building helpers
    │   ├── ValidationUtils.java                # Reusable validation checks
    │   ├── ControllerResponseUtils.java        # ResponseEntity construction helpers
    │   └── ExceptionHandlingUtils.java         # Exception message extraction helpers
    │
    └── 🎯 EshopApplication.java               # SYSTEM ENTRY POINT (@SpringBootApplication)
```

---

## ⚠️ Superseded Locations (Kept for Backwards Compatibility)

These old files still exist and compile but should NOT be referenced in new code. Migrate imports to their new `core/` counterparts:

| Old Location | New Location | Status |
|---|---|---|
| `aspect/Auditable.java` | `core/audit/Auditable.java` | ⚠️ Use new location |
| `aspect/AuditLoggingAspect.java` | `core/audit/AuditLoggingAspect.java` | ⚠️ Use new location |
| `interceptor/IdempotencyInterceptor.java` | `core/outbox/IdempotencyInterceptor.java` | ⚠️ Use new location |
| `scheduler/CacheWarmingScheduler.java` | `core/jobs/CacheWarmingJob.java` | ⚠️ Use new location |
| `event/LowStockEvent.java` | `core/events/domain/LowStockEvent.java` | ⚠️ Use new location |
| `event/ProductCreatedEvent.java` | `core/events/domain/ProductCreatedEvent.java` | ⚠️ Use new location |
| `event/StockChangedEvent.java` | `core/events/domain/StockChangedEvent.java` | ⚠️ Use new location |
| `event/ProductEventListener.java` | `core/events/listeners/ProductEventListener.java` | ⚠️ Use new location |
| `service/cache/ResilientRedisCacheService.java` | `core/cache/ResilientRedisCacheService.java` | ⚠️ Use new location |

---

## 📐 Architectural Laws (MANDATORY)

| # | Law | Rationale |
|---|-----|-----------|
| 1 | **No entity leakage** | JPA entities never reach controllers. Always convert via MapStruct DTOs. |
| 2 | **@Transactional at Service layer ONLY** | Controllers and Repositories never declare transactions. |
| 3 | **@Auditable on all admin/financial ops** | `AuditLoggingAspect` handles async persistence automatically. |
| 4 | **Admin is an Actor, not a Module** | Admin endpoints live inside domain controllers under `/admin/` paths. |
| 5 | **`core/` is infrastructure only** | Zero business logic in `core/`. Domain logic belongs in `service/`. |
| 6 | **Event listeners are side-effect only** | `core/events/listeners/` must not modify entities or execute business rules. |
| 7 | **`util/` is pure and stateless** | No Spring beans, no DB access, no HTTP context in `util/`. |
| 8 | **API errors use ApiError format always** | All errors must conform to `dto/error/ApiError.java`. Never expose raw stack traces. |
| 9 | **Flyway owns the schema** | Never modify DB schema outside a Flyway migration file. |
| 10 | **No hardcoded values** | All config via `@ConfigurationProperties` or environment variables. |
| 11 | **Scripts as Source Sets** | Standalone ETL/migration scripts must reside in `scripts/` and be registered as Gradle source sets to ensure dependency resolution and IDE support. |
