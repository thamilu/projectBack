# Database & Infrastructure Operations SOP

## 1. Correct Startup Sequence
The system relies on a specific order of initialization. The docker-compose file handles most dependencies, but understanding the flow is critical for troubleshooting.

### The Flow:
1.  **PostgreSQL Container Starts** (`eshop-postgres-dev`)
    *   Must become "healthy" (accepting connections).
2.  **Initialization Script Runs** (`docker/postgres/init-databases.sh`)
    *   This script runs **ONLY** if the database volume is empty (first run).
    *   It creates `eshop_app` (backend) and `eshop_Dev` (Keycloak) databases.
3.  **Keycloak Starts** (`eshop-keycloak-dev`)
    *   Waits for Postgres to be healthy.
    *   Connects to `eshop_Dev`.
    *   Imports realm (if properly configured) on first start.
4.  **Backend Application Starts** (`eshop-backend`)
    *   Waits for Postgres.
    *   Hibernate validates or creates schema in `eshop_app` based on `ddl-auto` setting.

## 2. Full Reset Procedure (Wipe Data)
If you need to completely reset the environment (e.g., after schema corruption or password changes):

```bash
# 1. Stop all containers
docker-compose -f docker-compose-dev.yml down

# 2. Remove volumes (CRITICAL for reset)
# This deletes all data in DB, Keycloak, etc.
docker volume rm eshop_back_postgres_dev_data
docker volume rm eshop_back_redis_dev_data
# ... remove other volumes as needed

# 3. Start Clean
docker-compose -f docker-compose-dev.yml up -d
```

## 3. Common Errors & Root Causes

### Error: `relation "seller_profiles" does not exist`
**Symptoms**: Backend 500 errors when accessing endpoints via Swapger/Postman.
**Possible Causes**:
1.  **Empty Database**: The `eshop_app` database exists but has no tables.
    *   *Fix*: Ensure `spring.jpa.hibernate.ddl-auto=create` or `update` is set in `application-dev.properties`. Restart backend.
2.  **Lazy Initialization**: If `spring.main.lazy-initialization=true`, the connection pool isn't tested at startup. If the DB connection fails silently later, the schema isn't created.
    *   *Fix*: Set `spring.main.lazy-initialization=false` to debug startup errors.
3.  **Startup Race Condition**: Backend started before Postgres was fully ready.

### Error: `FATAL: password authentication failed for user "postgres"`
**Symptoms**: Application fails to start; Hibernate throws JDBC exceptions.
**Possible Causes**:
1.  **Volume Mismatch**: You changed `POSTGRES_PASSWORD` in `docker-compose.yml`, but the existing Docker volume still has the **OLD** password stored on disk.
    *   *Fix*: You MUST delete the postgres volume (`docker volume rm ...`) to apply a new password.

### Error: `Connection refused` (during startup)
**Symptoms**: Keycloak or Backend crashes immediately.
**Cause**: The service tried to connect before the database was ready.
*   *Fix*: Ensure `healthcheck` and `depends_on` (condition: service_healthy) are correctly configured in `docker-compose.yml`.

## 4. Verification Steps
After a reset, always verify:

1.  **Databases Exist**:
    ```bash
    docker exec -it eshop-postgres-dev psql -U postgres -c "\l"
    # Should list eshop_app and eshop_Dev
    ```
2.  **Tables Exist**:
    ```bash
    docker exec -it eshop-postgres-dev psql -U postgres -d eshop_app -c "\dt"
    # Should list 50+ tables (users, products, etc.)
    ```
