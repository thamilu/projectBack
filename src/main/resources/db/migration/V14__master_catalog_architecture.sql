-- ============================================================
-- V14: Master Product Catalog & Seller Listing Architecture
-- Enterprise eCommerce Catalog schema evolution
-- ============================================================

-- 1. Create master_products table
CREATE TABLE IF NOT EXISTS master_products (
    id                     BIGSERIAL       PRIMARY KEY,
    name                   VARCHAR(255)    NOT NULL,
    slug                   VARCHAR(255)    NOT NULL,
    brand_id               BIGINT          NULL REFERENCES brands(id),
    category_id            BIGINT          NULL REFERENCES categories(id),
    base_description       TEXT            NULL,
    short_description      VARCHAR(500)    NULL,
    specifications         TEXT            NULL,
    approval_status        VARCHAR(30)     NOT NULL DEFAULT 'APPROVED',
    is_active              BOOLEAN         NOT NULL DEFAULT TRUE,
    created_by_seller_id   VARCHAR(100)    NULL,
    created_at             TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at             TIMESTAMP       NULL,
    created_by             VARCHAR(100)    NULL,
    updated_by             VARCHAR(100)    NULL,
    version                BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT uk_master_product_slug UNIQUE (slug)
);

-- 2. Create product_media table
CREATE TABLE IF NOT EXISTS product_media (
    id                  BIGSERIAL       PRIMARY KEY,
    master_product_id   BIGINT          NOT NULL REFERENCES master_products(id) ON DELETE CASCADE,
    media_url           VARCHAR(1000)   NOT NULL,
    media_type          VARCHAR(30)     NOT NULL DEFAULT 'IMAGE',
    is_primary          BOOLEAN         NOT NULL DEFAULT FALSE,
    sort_order          INTEGER         NOT NULL DEFAULT 0,
    alt_text            VARCHAR(255)    NULL,
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 3. Add columns to products table representing seller-listing specifics and overrides
ALTER TABLE products ADD COLUMN IF NOT EXISTS master_product_id BIGINT REFERENCES master_products(id);
ALTER TABLE products ADD COLUMN IF NOT EXISTS custom_title VARCHAR(255) NULL;
ALTER TABLE products ADD COLUMN IF NOT EXISTS custom_description TEXT NULL;
ALTER TABLE products ADD COLUMN IF NOT EXISTS custom_short_description VARCHAR(500) NULL;

-- 4. Create indexes for master product catalog performance
CREATE INDEX IF NOT EXISTS idx_master_product_category ON master_products(category_id);
CREATE INDEX IF NOT EXISTS idx_master_product_brand ON master_products(brand_id);
CREATE INDEX IF NOT EXISTS idx_master_product_status ON master_products(approval_status, is_active);
CREATE INDEX IF NOT EXISTS idx_product_master_id ON products(master_product_id);
CREATE INDEX IF NOT EXISTS idx_product_media_master_id ON product_media(master_product_id);

-- 5. Data Migration: Populate master_products from existing products
INSERT INTO master_products (
    name, slug, brand_id, category_id, base_description, short_description, specifications,
    approval_status, is_active, created_by_seller_id, created_at, updated_at, created_by, updated_by, version
)
SELECT 
    name, 
    COALESCE(friendly_url, 'prod-legacy-' || id), 
    brand_id, 
    category_id, 
    description, 
    short_description, 
    specifications,
    'APPROVED', 
    TRUE, 
    created_by, 
    COALESCE(created_at, CURRENT_TIMESTAMP), 
    updated_at, 
    created_by, 
    updated_by, 
    0
FROM products;

-- 6. Link existing products to the newly created master_products
UPDATE products p
SET master_product_id = m.id
FROM master_products m
WHERE m.slug = COALESCE(p.friendly_url, 'prod-legacy-' || p.id);

-- 7. Populate product_media from existing product_images
INSERT INTO product_media (master_product_id, media_url, media_type, is_primary, sort_order, alt_text, created_at)
SELECT 
    p.master_product_id, 
    pi.url, 
    'IMAGE', 
    COALESCE(pi.is_primary, FALSE), 
    COALESCE(pi.sort_order, 0), 
    pi.alt_text, 
    COALESCE(pi.created_at, CURRENT_TIMESTAMP)
FROM product_images pi
JOIN products p ON pi.product_id = p.id
WHERE p.master_product_id IS NOT NULL;
