-- =============================================
-- V12: Create media_asset table
-- Enterprise media management with UUID PK,
-- soft delete, upload status tracking, and
-- orphan cleanup support.
-- =============================================

CREATE TABLE IF NOT EXISTS media_asset (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id      BIGINT          NULL,
    original_filename VARCHAR(255)  NOT NULL,
    storage_key     VARCHAR(500)    NOT NULL,
    cdn_url         VARCHAR(1000)   NULL,
    thumbnail_url   VARCHAR(1000)   NULL,
    mime_type       VARCHAR(100)    NOT NULL,
    width           INTEGER         NULL,
    height          INTEGER         NULL,
    file_size       BIGINT          NOT NULL DEFAULT 0,
    is_primary      BOOLEAN         NOT NULL DEFAULT FALSE,
    upload_status   VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    uploaded_by     VARCHAR(255)    NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NULL,
    deleted_at      TIMESTAMP       NULL,
    version         BIGINT          NOT NULL DEFAULT 0,

    CONSTRAINT fk_media_asset_product
        FOREIGN KEY (product_id)
        REFERENCES products(id)
        ON DELETE SET NULL
);

-- =============================================
-- INDEXES
-- =============================================

-- Product lookup (most common query)
CREATE INDEX IF NOT EXISTS idx_media_asset_product_id
    ON media_asset(product_id)
    WHERE deleted_at IS NULL;

-- Unique storage key (prevents duplicate uploads)
CREATE UNIQUE INDEX IF NOT EXISTS uk_media_asset_storage_key
    ON media_asset(storage_key);

-- Orphan cleanup: find PENDING assets older than threshold
CREATE INDEX IF NOT EXISTS idx_media_asset_orphan_cleanup
    ON media_asset(upload_status, created_at)
    WHERE deleted_at IS NULL;

-- Primary image per product (fast primary lookup)
CREATE INDEX IF NOT EXISTS idx_media_asset_primary
    ON media_asset(product_id, is_primary)
    WHERE deleted_at IS NULL AND is_primary = TRUE;

-- Uploaded by (audit queries)
CREATE INDEX IF NOT EXISTS idx_media_asset_uploaded_by
    ON media_asset(uploaded_by);

-- Created at for time-based queries
CREATE INDEX IF NOT EXISTS idx_media_asset_created_at
    ON media_asset(created_at DESC);
