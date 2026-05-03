#!/bin/bash
# ============================================
# E-SHOP POSTGRESQL INITIALIZATION SCRIPT
# ============================================
# This script runs automatically when the PostgreSQL container
# is first created. It creates multiple databases for the application.
#
# Databases:
# - eshop_db: Used by Spring Boot application
# - eshop_keycloak: Used by Keycloak
# ============================================

set -e

# Create databases
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    -- Create Application database
    SELECT 'CREATE DATABASE eshop_db'
    WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'eshop_db')\gexec
    
    -- Create Keycloak database
    SELECT 'CREATE DATABASE eshop_keycloak'
    WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'eshop_keycloak')\gexec
    
    \echo '✓ Database eshop_db: Created/Exists'
    \echo '✓ Database eshop_keycloak: Created/Exists'
EOSQL
