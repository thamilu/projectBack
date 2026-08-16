-- ============================================================
-- V16: Add self-referencing relationship and governance columns to master products
-- ============================================================

ALTER TABLE master_products 
    ADD COLUMN parent_master_product_id BIGINT NULL REFERENCES master_products(id),
    ADD COLUMN root_master_product_id BIGINT NULL REFERENCES master_products(id),
    ADD COLUMN product_type VARCHAR(30) NOT NULL DEFAULT 'MASTER',
    ADD COLUMN derived_from_seller_id VARCHAR(100) NULL,
    ADD COLUMN derived_reason VARCHAR(500) NULL,
    ADD COLUMN created_from_catalog BOOLEAN NOT NULL DEFAULT FALSE,
    ALTER COLUMN name DROP NOT NULL,
    ALTER COLUMN category_id DROP NOT NULL;

CREATE INDEX idx_master_product_parent_id 
    ON master_products(parent_master_product_id);

CREATE INDEX idx_master_product_root_id 
    ON master_products(root_master_product_id);

CREATE INDEX idx_master_product_type 
    ON master_products(product_type);
