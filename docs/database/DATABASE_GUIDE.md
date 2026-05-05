

# --- File: DATABASE_ACCESS.md ---

# Database Access Guide

This guide explains how to access, manage, and reset the PostgreSQL databases. The application and Keycloak use **separate databases** within the same PostgreSQL container.

## 1. Connection Overview
Both databases run on the same PostgreSQL instance (`eshop-postgres-dev`).

**Common Connection Details:**
- **Host (Local):** `localhost`
- **Host (Docker):** `postgres`
- **Port:** `5432` (Local & Docker)
- **User:** `postgres`
- **Password:** `thamilu*884*`

**Database Names:**
- **Application Data:** `eshop_db` (Products, Orders, Users, etc.)
- **Keycloak Data:** `eshop_keycloak` (Authentication specific tables)

---

## 2. Access via PgAdmin (Recommended)
This project includes a pre-configured **PgAdmin** container (`eshop-pgadmin-dev`).

### Step 1: Ensure Containers are Running
Run the following command in your terminal:
```bash
docker-compose -f docker-compose-dev.yml up -d pgadmin postgres
```

### Step 2: Open PgAdmin
1. Open your browser and go to: [http://localhost:5050](http://localhost:5050)
2. Login with the default credentials:
   - **Email:** `admin@eshop.com`
   - **Password:** `admin`

> **Note:** If prompted to **"Set Master Password"**, this is for securing your saved connections. You can set it to anything you like (e.g., `admin`). Just don't forget it!

### Step 3: Connect to Server
1. In the PgAdmin dashboard, right-click on **Servers** > **Register** > **Server...**
2. **General Tab:**
   - Name: `Eshop Local`
3. **Connection Tab:**
   - **Host name/address:** `postgres` (If running in Docker) or `localhost` (If connected directly)
   - **Port:** `5432`
   - **Maintenance database:** `postgres`
   - **Username:** `postgres`
   - **Password:** `thamilu*884*`
   - **Save Password:** Toggle ON
4. Click **Save**.

### Step 4: Browse Databases
- Expand the server you just added.
- **For App Data:** Go to `Databases` > **`eshop_db`** > `Schemas` > `public` > `Tables`.
- **For Keycloak Data:** Go to `Databases` > **`eshop_keycloak`** > `Schemas` > `public` > `Tables`.

> **Note:** If you don't see `eshop_app`, right-click on `Databases` and select **Refresh**.

---

## 3. Resetting / Deleting Data

### Method 1: Automatic Reset on Startup (Dev Default)
The application is configured with `script.jpa.hibernate.ddl-auto=create` (or `create-drop`).
- **How to Reset:** Simply **Restart** the Spring Boot application.
- **What happens:** It drops all existing tables in `eshop_app` and recreates them empty on startup.

### Method 2: Manual Wipe via PgAdmin (Selective)
If you want to clear data without restarting:
1.  Open **PgAdmin**.
2.  Navigate to `Databases` > `eshop_db` > `Schemas` > `public`.
3.  Right-click on `public` schema -> **Delete/Drop** -> **Cascade**.
4.  Right-click on `Databases` > `eshop_db` -> `Create` -> `Schema` (Name it `public` again).
5.  Or run this SQL in Query Tool:
    ```sql
    DROP SCHEMA public CASCADE;
    CREATE SCHEMA public;
    ```

### Method 3: Complete Reset (Hard Reset)
To delete **EVERYTHING** (including Keycloak users, realms, and app data) and start fresh:

1.  Stop the containers and remove volumes:
    ```bash
    docker-compose -f docker-compose-dev.yml down -v
    ```
2.  Start them again:
    ```bash
    docker-compose -f docker-compose-dev.yml up -d
    ```
    *This will delete all data permanently.*

---

## 4. Access via External Tool (DBeaver, IntelliJ, etc.)
If you use tools like DBeaver or IntelliJ Database Tool:

### Connection Settings
- **Host:** `localhost`
- **Port:** `5432`
- **Username:** `postgres`
- **Password:** `thamilu*884*`
- **Database:** `eshop_db` (or `eshop_keycloak`)

### Troubleshooting
- If connection fails, ensure port 5432 is not blocked by Windows Firewall or another local Postgres instance.

---

## 5. Access via Command Line
You can access the databases directly inside the container.

**To access the Application Database:**
```bash
docker exec -it eshop-postgres-dev psql -U postgres -d eshop_db
```

**To access the Keycloak Database:**
```bash
docker exec -it eshop-postgres-dev psql -U postgres -d eshop_keycloak
```

**Common Commands:**
- `\dt` : List all tables
- `\d table_name` : Describe table structure
- `SELECT * FROM table_name LIMIT 10;` : View data
- `\q` : Quit


# --- File: DATABASE_OPERATIONS_SOP.md ---

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


# --- File: HIBERNATE_DDL_GUIDANCE.md ---

# Database Schema Management in Spring Boot: `ddl-auto=update` vs. Production Best Practices

## What is `spring.jpa.hibernate.ddl-auto=update`?

- The `update` option tells Hibernate to automatically adjust the database schema to match your JPA entity classes on every application startup.
- It will add new tables/columns, update column types, and sometimes drop or alter existing columns to fit your model.

## Why is `update` Unsafe for Production?

- **Data Loss Risk:** If you change a column type or reduce its length, Hibernate may drop and recreate the column, causing data loss.
- **No Rollback:** There is no way to undo schema changes if something goes wrong.
- **No Migration History:** You cannot track or audit what changes were made to the schema.
- **Inconsistent Environments:** In clustered or multi-instance deployments, schema changes may be applied at different times, causing inconsistencies.
- **Limited Capabilities:** Complex changes (renaming columns, splitting tables, data migrations) are not supported.

## What Should You Use in Production?

- **Recommended:** `spring.jpa.hibernate.ddl-auto=validate`
  - This setting checks that the database schema matches your entities, but does not make any changes.
  - If there is a mismatch, the application will fail to start, alerting you to the problem.

- **Best Practice:** Use a database migration tool such as **Flyway** or **Liquibase**.
  - Write migration scripts for every schema change.
  - Scripts are versioned, peer-reviewed, and tested before deployment.
  - You can roll back changes if needed.
  - Migration history is tracked in a special table.

## Example Configuration

**application.properties** (Production)
```properties
spring.jpa.hibernate.ddl-auto=validate
```

**application-dev.properties** (Development)
```properties
spring.jpa.hibernate.ddl-auto=update
```

## Summary Table

| Setting      | Use Case         | Data Loss Risk | Rollback | Migration History | Recommended for Production? |
|--------------|------------------|---------------|----------|-------------------|-----------------------------|
| create-drop  | Testing only     | High          | No       | No                | No                          |
| update       | Development only | Medium        | No       | No                | No                          |
| validate     | Production       | None          | N/A      | N/A               | Yes                         |
| none         | Manual control   | None          | N/A      | N/A               | Yes (with migration tool)   |

## Conclusion

- Use `update` only in development for rapid prototyping.
- Never use `update` or `create-drop` in production.
- Use `validate` in production and manage schema changes with Flyway or Liquibase.

