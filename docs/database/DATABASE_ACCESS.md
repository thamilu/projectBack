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
