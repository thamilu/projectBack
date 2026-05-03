-- ============================================================================
-- ShedLock Table for Distributed Scheduling
-- ============================================================================
-- Purpose: Ensure scheduled tasks run only once in clustered environments
-- Used by: ShedLock library for distributed locking
-- ============================================================================

CREATE TABLE IF NOT EXISTS shedlock (
    name VARCHAR(64) NOT NULL PRIMARY KEY,
    lock_until TIMESTAMP(3) NOT NULL,
    locked_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    locked_by VARCHAR(255) NOT NULL
);

-- Index for query performance
CREATE INDEX IF NOT EXISTS idx_shedlock_lock_until 
    ON shedlock (lock_until);

-- Add comment
COMMENT ON TABLE shedlock IS 'Distributed lock table for scheduled tasks in clustered environment';
COMMENT ON COLUMN shedlock.name IS 'Unique name of the scheduled task';
COMMENT ON COLUMN shedlock.lock_until IS 'Lock expiration timestamp';
COMMENT ON COLUMN shedlock.locked_at IS 'When the lock was acquired';
COMMENT ON COLUMN shedlock.locked_by IS 'Hostname/instance that acquired the lock';
