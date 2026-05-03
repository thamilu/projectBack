# PostgreSQL Database Management

This directory contains scripts for managing the E-Shop PostgreSQL databases.

## Databases

- **eshop_Dev**: Used by Keycloak for authentication/authorization data
- **eshop_app**: Used by Spring Boot application for business data

## Scripts

### `init-databases.sql`
Automatically creates both databases when the PostgreSQL container starts for the first time.

**Usage**: Automatically executed by Docker on container initialization.

### `drop-databases.sql`
Drops both databases and all their data.

**Usage**: Run manually when you need to reset databases:

```powershell
# Connect to PostgreSQL container and run the script
docker exec -i eshop-postgres-dev psql -U postgres -f /docker-entrypoint-initdb.d/drop-databases.sql

# Or run it directly from host
docker exec -i eshop-postgres-dev psql -U postgres < docker/postgres/drop-databases.sql
```

## Quick Commands

### Create databases manually
```powershell
docker exec -i eshop-postgres-dev psql -U postgres -c "CREATE DATABASE eshop_Dev;"
docker exec -i eshop-postgres-dev psql -U postgres -c "CREATE DATABASE eshop_app;"
```

### Drop databases manually
```powershell
docker exec -i eshop-postgres-dev psql -U postgres -c "DROP DATABASE IF EXISTS eshop_Dev;"
docker exec -i eshop-postgres-dev psql -U postgres -c "DROP DATABASE IF EXISTS eshop_app;"
```

### List all databases
```powershell
docker exec -i eshop-postgres-dev psql -U postgres -c "\l"
```

### Complete reset (nuclear option)
```powershell
# Stop and remove containers with volumes
docker-compose -f docker-compose-dev.yml down -v

# Start fresh (databases will be auto-created)
docker-compose -f docker-compose-dev.yml up -d postgres
```

## Notes

- The `init-databases.sql` script only runs when the PostgreSQL data volume is empty (first startup)
- If you need to re-run initialization, you must delete the volume: `docker volume rm eshop_back_postgres_dev_data`
- Always backup important data before running drop scripts
