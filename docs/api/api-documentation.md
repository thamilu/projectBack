---
# E-Shop API Documentation

## Overview
This document describes the main endpoints, security, and architecture of the E-Shop API.

### OpenAPI/Swagger UI
- Interactive API docs: `/swagger-ui.html` or `/v3/api-docs`

### Main Features
- Product CRUD, search, batch, and analytics endpoints
- DTO-based architecture (no entity exposure)
- Caching, rate limiting, and retry logic
- Full-text search (PostgreSQL tsvector)
- Security: JWT, RBAC, input validation, rate limiting

### Security Best Practices
- All endpoints require authentication (JWT) unless explicitly marked public.
- Role-based access control via `@PreAuthorize` and method-level security.
- Input validation on all request DTOs and parameters.
- Rate limiting on sensitive endpoints (search, create, batch).
- ETag and If-Match/If-None-Match for safe updates and caching.

### Monitoring & Logging
- All major actions are logged with user context and operation type.
- Micrometer metrics for create/update/delete/stock actions.
- Audit logs for create/update/delete with user info.

### Architecture
- Layered: Controller → Service → Repository → Entity
- DTOs for all API input/output
- MapStruct for mapping
- Caching: Caffeine, multi-level
- Retry: Spring Retry
- Full-text: PostgreSQL tsvector

---
For further details, see code and Swagger UI.