-- ============================================
-- E-SHOP POSTGRESQL INITIALIZATION SCRIPT
-- ============================================
-- This script runs automatically when the PostgreSQL container
-- is first created. It creates multiple databases for the application.
--
-- Databases:
-- - eshop_Dev: Used by Keycloak for authentication/authorization
-- - eshop_app: Used by Spring Boot application for business data
-- ============================================

-- Create Keycloak database (if not exists)
DO
$$
BEGIN
   IF NOT EXISTS (SELECT FROM pg_database WHERE datname = 'eshop_Dev') THEN
      PERFORM dblink_exec('dbname=' || current_database(), 'CREATE DATABASE eshop_Dev');
   END IF;
END
$$;

-- Create Application database (if not exists)
DO
$$
BEGIN
   IF NOT EXISTS (SELECT FROM pg_database WHERE datname = 'eshop_app') THEN
      PERFORM dblink_exec('dbname=' || current_database(), 'CREATE DATABASE eshop_app');
   END IF;
END
$$;

-- Display confirmation
\echo '✓ Database eshop_Dev created (Keycloak)'
\echo '✓ Database eshop_app created (Spring Boot Application)'
\echo '✓ All databases initialized successfully!'
