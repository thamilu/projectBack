-- Add missing columns for optimistic locking, soft delete, and audit
ALTER TABLE products
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS deleted BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS created_by VARCHAR(100),
    ADD COLUMN IF NOT EXISTS updated_by VARCHAR(100);

-- Fix boolean columns to use primitive boolean (not nullable)
ALTER TABLE products
    ALTER COLUMN featured SET DEFAULT false,
    ALTER COLUMN active SET DEFAULT true;

-- Update existing null values
UPDATE products SET featured = false WHERE featured IS NULL;
UPDATE products SET active = true WHERE active IS NULL;

-- Make boolean columns not null
ALTER TABLE products
    ALTER COLUMN featured SET NOT NULL,
    ALTER COLUMN active SET NOT NULL;

-- Add constraints
ALTER TABLE products
    ADD CONSTRAINT check_price_positive CHECK (price > 0),
    ADD CONSTRAINT check_discount_valid CHECK (discount_price IS NULL OR discount_price >= 0),
    ADD CONSTRAINT check_stock_non_negative CHECK (stock_quantity IS NULL OR stock_quantity >= 0);

-- Add unique constraint to SKU if not exists
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'unique_sku') THEN
        ALTER TABLE products ADD CONSTRAINT unique_sku UNIQUE (sku);
    END IF;
END$$;

-- Create indexes for better query performance
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_product_sku ON products(sku) WHERE sku IS NOT NULL;
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_product_category ON products(category_id);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_product_brand ON products(brand_id);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_product_shop ON products(shop_id);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_product_active ON products(active) WHERE active = true;
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_product_featured ON products(featured) WHERE featured = true;
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_product_deleted ON products(deleted) WHERE deleted = false;
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_product_price ON products(price);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_product_created ON products(created_at DESC);

-- Composite indexes for common queries
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_product_active_category 
    ON products(active, category_id) WHERE deleted = false;

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_product_active_featured 
    ON products(active, featured) WHERE deleted = false AND active = true;
