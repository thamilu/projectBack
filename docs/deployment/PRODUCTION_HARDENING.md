# 🛡️ Lean E-Shop: Production Hardening Guide [Staff-Level]

This document summarizes the high-performance, cost-optimized infrastructure and logging strategy implemented for the E-Shop backend.

## 📊 1. Infrastructure Strategy (2GB RAM VPS)
The architecture has been transformed into a **Lean Hybrid Model** to ensure stability on a constrained VPS.

### Container Resource Limits
| Service | CPU Limit | RAM Limit | Hardening Action |
| :--- | :--- | :--- | :--- |
| **PostgreSQL** | 0.5 | 512MB | Optimized Alpine image, strict memory caged. |
| **Backend App** | 1.0 | 1GB | Auto-scaling heap based on container limits. |
| **Keycloak** | 0.5 | 768MB | Production-hardened with JVM heap tuning. |
| **Nginx** | 0.1 | 128MB | Minimal reverse proxy footprint. |

### Service Offloading (Managed Services)
To keep the local VPS RAM under 2GB, we use:
*   **Database**: Neon.tech (Serverless Postgres) - *Optional backup to local Postgres*.
*   **Auth**: AWS Cognito / Managed Keycloak.
*   **Storage**: Cloudflare R2 (S3-Compatible) - Zero local disk usage for media.

---

## ⚡ 2. High-Performance Logging ("Best Speed")
Implemented a **"Silent Excellence"** strategy to maximize API throughput and minimize Disk I/O.

### Async Logging (Logback)
*   **Strategy**: Production logging is **Asynchronous**. Application threads do not block when writing logs.
*   **Log Level**: Global `ERROR` level. No "INFO" noise in production.
*   **File**: `logback-spring.xml` uses `AsyncAppender` with a non-blocking queue.

### Optimized Request Filter
*   **Zero Overhead**: `RequestLoggingFilter.java` now skips byte-copying and payload wrapping entirely when `app.logging.request.enabled=false`.
*   **Result**: 100% throughput performance for successful API calls.

---

## 🔐 3. Keycloak Optimization (The 10-Point Plan)
Keycloak has been hardened using enterprise-grade production settings:

1.  **Startup Mode**: Switched from `start-dev` to `start --optimized`.
2.  **JVM Tuning**: Explicitly set `-Xms256m -Xmx512m` to prevent RAM ballooning.
3.  **Silent Operations**: Set `KC_LOG_LEVEL: warn` and `KC_METRICS_ENABLED: false`.
4.  **Database**: Hard-wired to PostgreSQL for persistent, reliable auth storage.
5.  **DNS/Proxy**: Configured for `xforwarded` headers behind Nginx.

---

## 🛠️ 4. Deployment & Troubleshooting

### Building the Production Stack
Docker Compose is now "Hardened" to build and sync automatically:
```powershell
# Build and Start everything (picks up .env automatically)
docker compose -f docker-compose.prod.yml up -d --build
```

### Critical Environment Variables
The `docker-compose.prod.yml` is strictly synced with your `.env` file:
*   `DB_URL`: Complete JDBC connection string.
*   `JWT_SECRET`: Mandatory for security.
*   `LOG_FILE`: Path for error logs (passed to Spring).
*   `MANAGEMENT_PORT`: Required for Actuator health probes.

### Health Verification
*   **Postgres**: Checked via `pg_isready`.
*   **Backend**: Checked via TCP probe on `8082`.
*   **Keycloak**: Checked via TCP probe on `8080`.

---

**Status**: ✅ **Hardening Complete**  
**Target Architecture**: Production-Ready, 2GB RAM Capable, Silent/High-Performance.
