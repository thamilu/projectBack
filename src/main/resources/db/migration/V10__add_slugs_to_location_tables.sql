-- ============================================================================
-- V10: Add Slugs to Location Master Tables
-- ============================================================================
-- Purpose : Add slug columns for SEO-friendly URLs and normalized lookups.
-- Standards: lowercase-hyphen format for slugs.
--            Unique constraints ensure logical integrity within parent scopes.
-- ============================================================================

-- 1. COUNTRIES
ALTER TABLE countries ADD COLUMN slug VARCHAR(100);
-- Set initial slugs for any existing data (e.g., India -> india)
UPDATE countries SET slug = LOWER(REPLACE(name, ' ', '-')) WHERE slug IS NULL;
ALTER TABLE countries ALTER COLUMN slug SET NOT NULL;
CREATE UNIQUE INDEX uq_countries_slug ON countries(slug);

-- 2. STATES
ALTER TABLE states ADD COLUMN slug VARCHAR(100);
UPDATE states SET slug = LOWER(REPLACE(name, ' ', '-')) WHERE slug IS NULL;
ALTER TABLE states ALTER COLUMN slug SET NOT NULL;
-- Slug must be unique within a country
CREATE UNIQUE INDEX uq_states_country_slug ON states(country_id, slug);

-- 3. DISTRICTS
ALTER TABLE districts ADD COLUMN slug VARCHAR(100);
UPDATE districts SET slug = LOWER(REPLACE(name, ' ', '-')) WHERE slug IS NULL;
ALTER TABLE districts ALTER COLUMN slug SET NOT NULL;
-- Slug must be unique within a state
CREATE UNIQUE INDEX uq_districts_state_slug ON districts(state_id, slug);

-- 4. TALUKS
ALTER TABLE taluks ADD COLUMN slug VARCHAR(100);
UPDATE taluks SET slug = LOWER(REPLACE(name, ' ', '-')) WHERE slug IS NULL;
ALTER TABLE taluks ALTER COLUMN slug SET NOT NULL;
-- Slug must be unique within a district
CREATE UNIQUE INDEX uq_taluks_district_slug ON taluks(district_id, slug);

-- Add comments for documentation
COMMENT ON COLUMN countries.slug IS 'SEO-friendly unique identifier (e.g. india)';
COMMENT ON COLUMN states.slug    IS 'SEO-friendly identifier unique within country (e.g. karnataka)';
COMMENT ON COLUMN districts.slug IS 'SEO-friendly identifier unique within state (e.g. dakshina-kannada)';
COMMENT ON COLUMN taluks.slug    IS 'SEO-friendly identifier unique within district (e.g. mangaluru)';
