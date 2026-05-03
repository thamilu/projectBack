-- Flyway migration: create analytics_events table
-- Version 1: create table and indexes

CREATE TABLE IF NOT EXISTS analytics_events (
  id BIGSERIAL PRIMARY KEY,
  event_type VARCHAR(50) NOT NULL,
  timestamp TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  event_date DATE NOT NULL,
  user_id BIGINT,
  shop_id BIGINT,
  product_id BIGINT,
  order_id BIGINT,
  category_id BIGINT,
  revenue NUMERIC(12,2),
  quantity INTEGER,
  value NUMERIC(12,2),
  session_id VARCHAR(100),
  ip_address VARCHAR(45),
  user_agent VARCHAR(500),
  referrer_url VARCHAR(500),
  metadata TEXT,
  created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT now() NOT NULL,
  updated_at TIMESTAMP WITHOUT TIME ZONE,
  created_by VARCHAR(255),
  updated_by VARCHAR(255),
  version BIGINT
);

-- Indexes to match entity annotations
CREATE INDEX IF NOT EXISTS idx_analytics_event_type ON analytics_events (event_type);
CREATE INDEX IF NOT EXISTS idx_analytics_user_id ON analytics_events (user_id);
CREATE INDEX IF NOT EXISTS idx_analytics_shop_id ON analytics_events (shop_id);
CREATE INDEX IF NOT EXISTS idx_analytics_product_id ON analytics_events (product_id);
CREATE INDEX IF NOT EXISTS idx_analytics_timestamp ON analytics_events (timestamp);
CREATE INDEX IF NOT EXISTS idx_analytics_date ON analytics_events (event_date);
CREATE INDEX IF NOT EXISTS idx_analytics_composite ON analytics_events (event_type, event_date);
