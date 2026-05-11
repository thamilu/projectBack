# ADR-003: Choice of PostgreSQL and Redis

## Date
2026-05-10

## Status
Accepted

## Context
E-commerce data is highly relational (Users have Orders, Orders have Products) and requires strong ACID guarantees. At the same time, the system needs to be fast, especially for repetitive reads (Product listings, sessions).

## Decision
We chose **PostgreSQL** as the primary relational database and **Redis** for distributed caching.

### Rationale:
1. **PostgreSQL**:
    - **Reliability**: Proven ACID compliance for transactional data.
    - **Extensibility**: Supports JSONB, Full-Text Search (eliminating immediate need for Elasticsearch), and advanced indexing.
    - **Performance**: Excellent query planner and concurrent performance.
2. **Redis**:
    - **Speed**: In-memory data store for sub-millisecond lookups.
    - **Resilience**: Configured with a "Resilient" wrapper in the app to fallback to Caffeine (local cache) if Redis is down.
    - **Versatility**: Used for caching, rate limiting, and distributed locking.

## Consequences
- **RAM**: PostgreSQL needs ~1-2GB; Redis needs 512MB-2GB depending on data size.
- **Maintenance**: Requires monitoring of DB connections (HikariCP) and cache hit ratios.
- **Complexity**: Requires careful cache invalidation strategies to prevent stale data.
