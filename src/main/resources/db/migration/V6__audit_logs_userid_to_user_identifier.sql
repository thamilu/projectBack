-- Flyway migration: drop FK from audit_logs.user_id (if present), rename column to user_identifier
-- and recreate index. This prevents incompatible varchar->bigint FK issues.
BEGIN;

-- Attempt to drop any FK that may reference users(id)
ALTER TABLE IF EXISTS audit_logs DROP CONSTRAINT IF EXISTS fkjs4iimve3y0xssbtve5ysyef0;
ALTER TABLE IF EXISTS audit_logs DROP CONSTRAINT IF EXISTS fk_audit_logs_user;

-- Drop old index if exists
DROP INDEX IF EXISTS idx_audit_user_id;

-- Rename column to user_identifier (avoid FK to users.id)
ALTER TABLE IF EXISTS audit_logs RENAME COLUMN IF EXISTS user_id TO user_identifier;

-- Recreate a safe index for the renamed column
CREATE INDEX IF NOT EXISTS idx_audit_user_identifier ON audit_logs(user_identifier);

COMMIT;
