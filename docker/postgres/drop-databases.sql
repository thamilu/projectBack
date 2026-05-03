-- ============================================
-- E-SHOP POSTGRESQL DATABASE CLEANUP SCRIPT
-- ============================================
-- This script drops both application databases
-- Use this when you want to completely reset your databases
--
-- WARNING: This will delete ALL data in both databases!
-- ============================================

-- Terminate all connections to the databases first
SELECT pg_terminate_backend(pg_stat_activity.pid)
FROM pg_stat_activity
WHERE pg_stat_activity.datname IN ('eshop_Dev', 'eshop_app')
  AND pid <> pg_backend_pid();

-- Drop databases
DROP DATABASE IF EXISTS eshop_app;
DROP DATABASE IF EXISTS eshop_Dev;

-- Display confirmation
\echo '✓ Database eshop_app dropped'
\echo '✓ Database eshop_Dev dropped'
\echo '✓ All databases cleaned up successfully!'
