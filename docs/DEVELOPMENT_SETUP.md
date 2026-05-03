# ≡ƒÜÇ E-Shop Development Setup Guide

This document contains all the information needed to run, manage, and access the development infrastructure for the E-Shop backend.

## ≡ƒôº Quick Start Commands

| Action | Command |
| :--- | :--- |
| **Start Infrastructure** | `docker-compose -f docker-compose-dev.yml up -d` |
| **Stop Infrastructure** | `docker-compose -f docker-compose-dev.yml down` |
| **Reset Databases** | `docker-compose -f docker-compose-dev.yml down -v; docker-compose -f docker-compose-dev.yml up -d` |
| **Run Backend** | `./gradlew.bat clean bootRun --args="--spring.profiles.active=dev"` |

---

## Γ£à Service Access Points

### 1. PostgreSQL Database
Available on port `5432` with two distinct databases:
*   **Application DB**: `eshop_db` (Where products, orders, and users are stored)
*   **Keycloak DB**: `eshop_keycloak` (Internal auth data)
*   **Username**: `postgres`
*   **Password**: `thamilu*884*`

### 2. How to Access the Database (Step-by-Step)

#### Method A: Using pgAdmin (Recommended)
1.  **Open Browser**: Go to [http://localhost:5050](http://localhost:5050).
2.  **Login**: User: `admin@eshop.com`, Pass: `admin`.
3.  **Register Server**:
    *   Right-click **Servers** > **Register** > **Server...**
    *   **General**: Name it `E-Shop Local`.
    *   **Connection**:
        *   **Host name/address**: `postgres` (if using Docker network) or `localhost` (from your PC).
        *   **Port**: `5432`
        *   **Username**: `postgres`
        *   **Password**: `thamilu*884*`
    *   Click **Save**.

#### Method B: Using External Tools (DBeaver, IntelliJ, etc.)
Use these settings for any external database manager:
*   **Host**: `localhost`
*   **Port**: `5432`
*   **User**: `postgres`
*   **Pass**: `thamilu*884*`
*   **Database Names**: `eshop_db` (App) or `eshop_keycloak` (Auth).

### 3. Keycloak (Authentication)
*   **Admin Console**: [http://localhost:8080](http://localhost:8080)
*   **Admin User**: `admin`
*   **Admin Password**: `Admin@@Secret123`
*   **Realms**:
    *   `eshop`: Marketplace users (Sellers, Customers)
    *   `eshop-admin`: Administration (Approvals)

### 4. MailHog (Email Testing)
Captures every email sent by the system (e.g., OTPs, order confirmations).
*   **Web UI**: [http://localhost:8025](http://localhost:8025)

### 5. Redis Commander (Cache Management)
*   **Web UI**: [http://localhost:8081](http://localhost:8081)

---

## ≡ƒôƒ Environment Variables (.env)
Your `.env` file is the master configuration. Key variables include:
*   `KEYCLOAK_AUTH_SERVER`: Base URL for authentication.
*   `DB_URL`: JDBC connection string for the backend.
*   `JWT_SECRET`: Randomly generated 512-bit key for session security.

> [!NOTE]
> Never commit your `.env` file to Git. Use `.env.example` as a template for new environments.
