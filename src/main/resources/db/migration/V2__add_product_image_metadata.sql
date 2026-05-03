-- Add storage metadata columns to product_images
ALTER TABLE product_images
    ADD COLUMN provider VARCHAR(50),
    ADD COLUMN public_id VARCHAR(500),
    ADD COLUMN thumbnail_url VARCHAR(500),
    ADD COLUMN width INTEGER,
    ADD COLUMN height INTEGER,
    ADD COLUMN file_size BIGINT;

-- Add indexes for quick lookups when deleting or querying by provider/public_id
CREATE INDEX IF NOT EXISTS idx_product_images_provider ON product_images(provider);
CREATE INDEX IF NOT EXISTS idx_product_images_public_id ON product_images(public_id);
