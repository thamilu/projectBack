-- Create product_seo table
CREATE TABLE product_seo (
    product_id BIGINT PRIMARY KEY,
    meta_title VARCHAR(160),
    meta_description VARCHAR(320),
    meta_keywords VARCHAR(500),
    canonical_url VARCHAR(1000),
    CONSTRAINT fk_product_seo_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);

-- Copy existing SEO data
INSERT INTO product_seo (product_id, meta_title, meta_description, meta_keywords, canonical_url)
SELECT id, meta_title, meta_description, meta_keywords, canonical_url FROM products;

-- Create product_dimensions table
CREATE TABLE product_dimensions (
    product_id BIGINT PRIMARY KEY,
    weight NUMERIC(12, 4),
    weight_unit VARCHAR(5) DEFAULT 'KG',
    length NUMERIC(10, 2),
    width NUMERIC(10, 2),
    height NUMERIC(10, 2),
    dimension_unit VARCHAR(5) DEFAULT 'CM',
    CONSTRAINT fk_product_dimensions_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);

-- Copy existing dimensions data
INSERT INTO product_dimensions (product_id, weight, weight_unit, length, width, height, dimension_unit)
SELECT id, weight, weight_unit, length, width, height, dimension_unit FROM products;

-- Create product_metrics table
CREATE TABLE product_metrics (
    product_id BIGINT PRIMARY KEY,
    view_count BIGINT DEFAULT 0,
    purchase_count BIGINT DEFAULT 0,
    wishlist_count INTEGER DEFAULT 0,
    popularity_score DOUBLE PRECISION DEFAULT 0.0,
    average_rating NUMERIC(3, 2) DEFAULT 0.00,
    review_count INTEGER DEFAULT 0,
    rating_5_count INTEGER DEFAULT 0,
    rating_4_count INTEGER DEFAULT 0,
    rating_3_count INTEGER DEFAULT 0,
    rating_2_count INTEGER DEFAULT 0,
    rating_1_count INTEGER DEFAULT 0,
    CONSTRAINT fk_product_metrics_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);

-- Copy existing metrics data
INSERT INTO product_metrics (product_id, view_count, purchase_count, wishlist_count, popularity_score, average_rating, review_count, rating_5_count, rating_4_count, rating_3_count, rating_2_count, rating_1_count)
SELECT id, view_count, purchase_count, wishlist_count, popularity_score, average_rating, review_count, rating_5_count, rating_4_count, rating_3_count, rating_2_count, rating_1_count FROM products;

-- Create digital_products table
CREATE TABLE digital_products (
    product_id BIGINT PRIMARY KEY,
    download_url VARCHAR(1000),
    download_limit INTEGER,
    download_expiry_days INTEGER,
    CONSTRAINT fk_digital_products_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);

-- Copy existing digital products data
INSERT INTO digital_products (product_id, download_url, download_limit, download_expiry_days)
SELECT id, download_url, download_limit, download_expiry_days FROM products WHERE is_digital = true;

-- Create product_subscriptions table
CREATE TABLE product_subscriptions (
    product_id BIGINT PRIMARY KEY,
    subscription_interval VARCHAR(20),
    subscription_interval_count INTEGER,
    trial_days INTEGER,
    CONSTRAINT fk_product_subscriptions_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);

-- Copy existing subscription data
INSERT INTO product_subscriptions (product_id, subscription_interval, subscription_interval_count, trial_days)
SELECT id, subscription_interval, subscription_interval_count, trial_days FROM products WHERE is_subscription = true;

-- Create product_warranty table
CREATE TABLE product_warranty (
    product_id BIGINT PRIMARY KEY,
    warranty_months INTEGER,
    warranty_description VARCHAR(1000),
    return_days INTEGER DEFAULT 30,
    return_policy VARCHAR(1000),
    is_returnable BOOLEAN DEFAULT true,
    CONSTRAINT fk_product_warranty_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);

-- Copy existing warranty data
INSERT INTO product_warranty (product_id, warranty_months, warranty_description, return_days, return_policy, is_returnable)
SELECT id, warranty_months, warranty_description, return_days, return_policy, is_returnable FROM products;

-- Create product_inventory_details table
CREATE TABLE product_inventory_details (
    product_id BIGINT PRIMARY KEY,
    stock_quantity INTEGER DEFAULT 0,
    reserved_quantity INTEGER DEFAULT 0,
    reorder_level INTEGER,
    reorder_quantity INTEGER,
    track_inventory BOOLEAN DEFAULT true,
    allow_backorder BOOLEAN DEFAULT false,
    backorder_lead_days INTEGER,
    stock_status VARCHAR(30) DEFAULT 'IN_STOCK',
    CONSTRAINT fk_product_inventory_details_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);

-- Copy existing inventory/stock data
INSERT INTO product_inventory_details (product_id, stock_quantity, reserved_quantity, reorder_level, reorder_quantity, track_inventory, allow_backorder, backorder_lead_days, stock_status)
SELECT id, stock_quantity, reserved_quantity, reorder_level, reorder_quantity, track_inventory, allow_backorder, backorder_lead_days, stock_status FROM products;

-- Drop old columns from products table
ALTER TABLE products 
DROP COLUMN meta_title,
DROP COLUMN meta_description,
DROP COLUMN meta_keywords,
DROP COLUMN canonical_url,
DROP COLUMN weight,
DROP COLUMN weight_unit,
DROP COLUMN length,
DROP COLUMN width,
DROP COLUMN height,
DROP COLUMN dimension_unit,
DROP COLUMN view_count,
DROP COLUMN purchase_count,
DROP COLUMN wishlist_count,
DROP COLUMN popularity_score,
DROP COLUMN average_rating,
DROP COLUMN review_count,
DROP COLUMN rating_5_count,
DROP COLUMN rating_4_count,
DROP COLUMN rating_3_count,
DROP COLUMN rating_2_count,
DROP COLUMN rating_1_count,
DROP COLUMN download_url,
DROP COLUMN download_limit,
DROP COLUMN download_expiry_days,
DROP COLUMN subscription_interval,
DROP COLUMN subscription_interval_count,
DROP COLUMN trial_days,
DROP COLUMN warranty_months,
DROP COLUMN warranty_description,
DROP COLUMN return_days,
DROP COLUMN return_policy,
DROP COLUMN is_returnable,
DROP COLUMN stock_quantity,
DROP COLUMN reserved_quantity,
DROP COLUMN reorder_level,
DROP COLUMN reorder_quantity,
DROP COLUMN track_inventory,
DROP COLUMN allow_backorder,
DROP COLUMN backorder_lead_days,
DROP COLUMN stock_status;
