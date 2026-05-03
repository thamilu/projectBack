# Backend Tech Stack - E-Shop

This document outlines the technologies, frameworks, and tools used in the **E-Shop Backend** service.

## Core Framework & Language
- **Language:** Java 21 (LTS)
- **Framework:** Spring Boot 4.0.2
- **Build Tool:** Gradle

## Persistence & Data Management
- **Primary Database:** PostgreSQL
- **ORM:** Hibernate / Spring Data JPA
- **Database Migrations:** Flyway (v10.10.0)
- **Validation:** Hibernate Validator (JSR 380)

## Security & Identity
- **Authentication/Authorization:** Spring Security
- **OAuth2/OIDC:** Spring Security OAuth2 (Resource Server & Client)
- **Identity Provider:** Keycloak (with Keycloak Admin Client integration)
- **JWT:** JJWT (io.jsonwebtoken) for custom token handling
- **XSS Prevention:** JSoup for HTML sanitization

## Caching & Performance
- **Distributed Cache:** Redis (Lettuce client with connection pooling)
- **Local Cache:** Caffeine (as a secondary/fallback cache)
- **Task Scheduling:** ShedLock (for distributed locks on scheduled tasks)

## Reliability & Resilience
- **Resilience4j:** 
  - Circuit Breaker
  - Rate Limiter
  - Retry Mechanism
  - Bulkhead
  - Time Limiter

## Monitoring & Observability
- **Metrics:** Micrometer / Prometheus (via Spring Boot Actuator)
- **Distributed Tracing:** Micrometer Tracing with Brave & Zipkin
- **Logging:** SLF4J with Logback

## API & Documentation
- **API Documentation:** Springdoc OpenAPI (Swagger UI v3)
- **REST Client:** Apache HttpComponents, Spring WebClient (WebFlux)

## Integrations & Services
- **Payment Gateways:**
  - Stripe
  - PayPal
  - Razorpay
- **Cloud Storage:** Cloudinary (Image storage and transformation)
- **Email:** Spring Boot Starter Mail
- **SEO/Utilities:** 
  - Sitemapgen4j (XML Sitemap generation)
  - Slugify (SEO-friendly URL generation)

## Utility Libraries
- **Mapping:** MapStruct (v1.6.3)
- **Boilerplate Reduction:** Project Lombok (v1.18.42)
- **File Handling:** Apache Tika (MIME type detection)
- **Image Processing:** Thumbnailator
- **Money Handling:** Moneta (JSR 354 implementation)
- **GDPR/Reporting:** Apache POI (Excel) and iText (PDF)

## Testing
- **Unit Testing:** JUnit 5, Mockito
- **Integration Testing:** Testcontainers (Dockerized PostgreSQL/Redis)
- **Security Testing:** Spring Security Test
- **API Testing:** Spring Boot Starter Test (MockMvc)
