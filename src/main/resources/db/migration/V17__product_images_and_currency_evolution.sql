-- ============================================================
-- V17: Product Images Architecture & Currency Evolution
-- ============================================================

-- 1. Restructure product_images table to reference master_products

-- Add master_product_id column (nullable first to allow migration)
ALTER TABLE product_images
    ADD COLUMN IF NOT EXISTS master_product_id BIGINT NULL REFERENCES master_products(id) ON DELETE CASCADE;

-- Migrate data: Set master_product_id from product_id through products table
UPDATE product_images pi
SET master_product_id = p.master_product_id
FROM products p
WHERE pi.product_id = p.id;

-- Prune any image records that cannot be associated with a valid master product to satisfy NOT NULL
DELETE FROM product_images WHERE master_product_id IS NULL;

-- Make master_product_id NOT NULL now that it is fully populated
ALTER TABLE product_images ALTER COLUMN master_product_id SET NOT NULL;

-- 2. Add storage_key and mime_type columns to product_images

ALTER TABLE product_images
    ADD COLUMN IF NOT EXISTS storage_key TEXT NULL,
    ADD COLUMN IF NOT EXISTS mime_type VARCHAR(100) NULL;

-- Populate storage_key and mime_type with sensible defaults derived from existing data
UPDATE product_images
SET storage_key = COALESCE(public_id, 'products/image_' || id),
    mime_type = CASE
        WHEN url LIKE '%.png' THEN 'image/png'
        WHEN url LIKE '%.webp' THEN 'image/webp'
        ELSE 'image/jpeg'
    END;

-- Make storage_key NOT NULL now that it is fully populated
ALTER TABLE product_images ALTER COLUMN storage_key SET NOT NULL;

-- 3. Enhance existing fields to align with recommended sizes

ALTER TABLE product_images
    ALTER COLUMN url TYPE TEXT,
    ALTER COLUMN alt_text TYPE VARCHAR(500),
    ALTER COLUMN image_type TYPE VARCHAR(50);

-- 4. Add soft delete columns
ALTER TABLE product_images
    ADD COLUMN IF NOT EXISTS deleted BOOLEAN DEFAULT FALSE NOT NULL,
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP WITH TIME ZONE NULL,
    ADD COLUMN IF NOT EXISTS deleted_by VARCHAR(100) NULL;

-- 5. Drop legacy relationship with listing products table
ALTER TABLE product_images
    DROP COLUMN IF EXISTS product_id;

-- 6. Add performance indexes for product_images
CREATE INDEX IF NOT EXISTS idx_product_images_master_product ON product_images(master_product_id);
CREATE INDEX IF NOT EXISTS idx_product_images_deleted ON product_images(deleted) WHERE deleted = false;

-- ========================================================================
-- 7. Create seller_product_images table for listing-specific overrides
-- ========================================================================
CREATE TABLE IF NOT EXISTS seller_product_images (
    id                  BIGSERIAL PRIMARY KEY,
    product_id          BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    url                 TEXT NOT NULL,
    storage_key         TEXT NOT NULL,
    alt_text            VARCHAR(500) NULL,
    mime_type           VARCHAR(100) NULL,
    image_type          VARCHAR(50) NULL DEFAULT 'GALLERY',
    width               INTEGER NULL,
    height              INTEGER NULL,
    file_size           BIGINT NULL,
    is_primary          BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order          INTEGER NOT NULL DEFAULT 0,
    active              BOOLEAN NOT NULL DEFAULT TRUE,
    deleted             BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at          TIMESTAMP WITH TIME ZONE NULL,
    deleted_by          VARCHAR(100) NULL,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP WITH TIME ZONE NULL,
    version             BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_seller_image_product ON seller_product_images(product_id);
CREATE INDEX IF NOT EXISTS idx_seller_image_deleted ON seller_product_images(deleted) WHERE deleted = false;

-- ========================================================================
-- 8. Add currency_code to stores table
-- ========================================================================
ALTER TABLE stores
    ADD COLUMN IF NOT EXISTS currency_code VARCHAR(3) NOT NULL DEFAULT 'INR';
