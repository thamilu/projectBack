-- Migration to add UUID and soft delete support to existing tables
-- Run this after deploying the new BaseEntity infrastructure

-- ========================================================================
-- USERS TABLE
-- ========================================================================
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS uuid VARCHAR(36),
    ADD COLUMN IF NOT EXISTS deleted BOOLEAN DEFAULT FALSE NOT NULL,
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS deleted_by VARCHAR(100);

-- Generate UUIDs for existing records
UPDATE users SET uuid = gen_random_uuid()::text WHERE uuid IS NULL;

-- Add unique constraint
ALTER TABLE users ADD CONSTRAINT uk_users_uuid UNIQUE (uuid);
ALTER TABLE users ALTER COLUMN uuid SET NOT NULL;

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_users_deleted ON users(deleted) WHERE deleted = false;
CREATE INDEX IF NOT EXISTS idx_users_uuid ON users(uuid);

-- ========================================================================
-- PRODUCTS TABLE
-- ========================================================================
ALTER TABLE products
    ADD COLUMN IF NOT EXISTS uuid VARCHAR(36),
    ADD COLUMN IF NOT EXISTS deleted BOOLEAN DEFAULT FALSE NOT NULL,
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS deleted_by VARCHAR(100);

UPDATE products SET uuid = gen_random_uuid()::text WHERE uuid IS NULL;
ALTER TABLE products ADD CONSTRAINT uk_products_uuid UNIQUE (uuid);
ALTER TABLE products ALTER COLUMN uuid SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_products_deleted ON products(deleted) WHERE deleted = false;
CREATE INDEX IF NOT EXISTS idx_products_uuid ON products(uuid);

-- ========================================================================
-- ORDERS TABLE
-- ========================================================================
ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS uuid VARCHAR(36),
    ADD COLUMN IF NOT EXISTS deleted BOOLEAN DEFAULT FALSE NOT NULL,
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS deleted_by VARCHAR(100);

UPDATE orders SET uuid = gen_random_uuid()::text WHERE uuid IS NULL;
ALTER TABLE orders ADD CONSTRAINT uk_orders_uuid UNIQUE (uuid);
ALTER TABLE orders ALTER COLUMN uuid SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_orders_deleted ON orders(deleted) WHERE deleted = false;
CREATE INDEX IF NOT EXISTS idx_orders_uuid ON orders(uuid);

-- ========================================================================
-- CATEGORIES TABLE
-- ========================================================================
ALTER TABLE categories
    ADD COLUMN IF NOT EXISTS uuid VARCHAR(36),
    ADD COLUMN IF NOT EXISTS deleted BOOLEAN DEFAULT FALSE NOT NULL,
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS deleted_by VARCHAR(100);

UPDATE categories SET uuid = gen_random_uuid()::text WHERE uuid IS NULL;
ALTER TABLE categories ADD CONSTRAINT uk_categories_uuid UNIQUE (uuid);
ALTER TABLE categories ALTER COLUMN uuid SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_categories_deleted ON categories(deleted) WHERE deleted = false;
CREATE INDEX IF NOT EXISTS idx_categories_uuid ON categories(uuid);

-- ========================================================================
-- Add similar migrations for other tables:
-- - order_items
-- - payments
-- - shipping_addresses
-- - reviews
-- - wishlists
-- - carts
-- - cart_items
-- - coupons
-- - inventory
-- ========================================================================

-- Example template for other tables:
-- ALTER TABLE <table_name>
--     ADD COLUMN IF NOT EXISTS uuid VARCHAR(36),
--     ADD COLUMN IF NOT EXISTS deleted BOOLEAN DEFAULT FALSE NOT NULL,
--     ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP,
--     ADD COLUMN IF NOT EXISTS deleted_by VARCHAR(100);
-- 
-- UPDATE <table_name> SET uuid = gen_random_uuid()::text WHERE uuid IS NULL;
-- ALTER TABLE <table_name> ADD CONSTRAINT uk_<table_name>_uuid UNIQUE (uuid);
-- ALTER TABLE <table_name> ALTER COLUMN uuid SET NOT NULL;
-- 
-- CREATE INDEX IF NOT EXISTS idx_<table_name>_deleted ON <table_name>(deleted) WHERE deleted = false;
-- CREATE INDEX IF NOT EXISTS idx_<table_name>_uuid ON <table_name>(uuid);
