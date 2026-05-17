-- ============================================================================
-- V11: Add postal_code_id to user_addresses
-- ============================================================================
-- Purpose : Link user addresses to the normalized postal_codes master table.
--           The existing free-text address columns are KEPT for migration safety.
--           They can be deprecated and removed in a future migration once all
--           data is backfilled via the frontend address flow.
-- ============================================================================

ALTER TABLE user_addresses
    ADD COLUMN IF NOT EXISTS postal_code_id BIGINT
        REFERENCES postal_codes(id) ON DELETE SET NULL;

COMMENT ON COLUMN user_addresses.postal_code_id
    IS 'FK to postal_codes master table. Null-safe — existing rows keep free-text fields.';

-- Index to support fast FK lookups and joins
CREATE INDEX IF NOT EXISTS idx_user_addresses_postal_code_id
    ON user_addresses(postal_code_id);
