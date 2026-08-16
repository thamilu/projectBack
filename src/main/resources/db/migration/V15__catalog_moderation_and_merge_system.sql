-- ============================================================
-- V15: Catalog Moderation, Duplicate Detection & Merge System
-- Schema additions to support marketplace duplicate product reviews
-- ============================================================

-- 1. Add fields to master_products
ALTER TABLE master_products ADD COLUMN IF NOT EXISTS duplicate_status VARCHAR(50) DEFAULT 'NONE';
ALTER TABLE master_products ADD COLUMN IF NOT EXISTS merged_into_product_id BIGINT REFERENCES master_products(id);

-- 2. Create product_duplicate_candidates table
CREATE TABLE IF NOT EXISTS product_duplicate_candidates (
    id                  BIGSERIAL       PRIMARY KEY,
    source_product_id   BIGINT          NOT NULL REFERENCES master_products(id) ON DELETE CASCADE,
    matched_product_id  BIGINT          NOT NULL REFERENCES master_products(id) ON DELETE CASCADE,
    similarity_score    DOUBLE PRECISION NOT NULL,
    review_status       VARCHAR(50)     NOT NULL DEFAULT 'PENDING',
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP       NULL
);

-- 3. Create indices for matching candidates performance
CREATE INDEX IF NOT EXISTS idx_duplicate_candidate_source ON product_duplicate_candidates(source_product_id);
CREATE INDEX IF NOT EXISTS idx_duplicate_candidate_status ON product_duplicate_candidates(review_status);
