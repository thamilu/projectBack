-- ============================================================================
-- V9: Location Master Tables
-- ============================================================================
-- Purpose : Establish normalized location hierarchy for address lookups.
--           Hierarchy: Country → State → District → Taluk → PostalCode
-- Standards: All tables include BaseEntity audit columns.
--            Unique constraints prevent logical duplicates.
--            Composite index on postal_codes for fast pincode lookups.
-- ============================================================================

-- ======================== COUNTRIES ========================

CREATE TABLE IF NOT EXISTS countries (
    id           BIGSERIAL    PRIMARY KEY,
    iso_code     VARCHAR(5)   NOT NULL,
    name         VARCHAR(100) NOT NULL,
    phone_code   VARCHAR(10),
    is_active    BOOLEAN      NOT NULL DEFAULT TRUE,
    version      BIGINT       NOT NULL DEFAULT 0,
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP,
    created_by   VARCHAR(255),
    updated_by   VARCHAR(255),

    CONSTRAINT uq_countries_iso_code UNIQUE (iso_code)
);

COMMENT ON TABLE  countries              IS 'Master table of countries — root of the location hierarchy';
COMMENT ON COLUMN countries.iso_code     IS 'ISO 3166-1 alpha-2 country code (e.g. IN, US)';
COMMENT ON COLUMN countries.phone_code   IS 'International dialing code (e.g. +91)';

-- ======================== STATES ========================

CREATE TABLE IF NOT EXISTS states (
    id           BIGSERIAL    PRIMARY KEY,
    country_id   BIGINT       NOT NULL REFERENCES countries(id),
    name         VARCHAR(100) NOT NULL,
    state_code   VARCHAR(10)  NOT NULL,
    is_active    BOOLEAN      NOT NULL DEFAULT TRUE,
    version      BIGINT       NOT NULL DEFAULT 0,
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP,
    created_by   VARCHAR(255),
    updated_by   VARCHAR(255),

    CONSTRAINT uq_states_country_code UNIQUE (country_id, state_code)
);

COMMENT ON TABLE  states            IS 'Master table of states / union territories within a country';
COMMENT ON COLUMN states.state_code IS 'ISO-style state abbreviation (e.g. KA, MH)';

CREATE INDEX IF NOT EXISTS idx_states_country_id ON states(country_id);

-- ======================== DISTRICTS ========================

CREATE TABLE IF NOT EXISTS districts (
    id           BIGSERIAL    PRIMARY KEY,
    state_id     BIGINT       NOT NULL REFERENCES states(id),
    name         VARCHAR(100) NOT NULL,
    is_active    BOOLEAN      NOT NULL DEFAULT TRUE,
    version      BIGINT       NOT NULL DEFAULT 0,
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP,
    created_by   VARCHAR(255),
    updated_by   VARCHAR(255),

    CONSTRAINT uq_districts_state_name UNIQUE (state_id, name)
);

COMMENT ON TABLE districts IS 'Master table of districts within a state';

CREATE INDEX IF NOT EXISTS idx_districts_state_id ON districts(state_id);

-- ======================== TALUKS ========================

CREATE TABLE IF NOT EXISTS taluks (
    id           BIGSERIAL    PRIMARY KEY,
    district_id  BIGINT       NOT NULL REFERENCES districts(id),
    name         VARCHAR(100) NOT NULL,
    is_active    BOOLEAN      NOT NULL DEFAULT TRUE,
    version      BIGINT       NOT NULL DEFAULT 0,
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP,
    created_by   VARCHAR(255),
    updated_by   VARCHAR(255),

    CONSTRAINT uq_taluks_district_name UNIQUE (district_id, name)
);

COMMENT ON TABLE taluks IS 'Master table of taluks / tehsils within a district';

CREATE INDEX IF NOT EXISTS idx_taluks_district_id ON taluks(district_id);

-- ======================== POSTAL CODES ========================

CREATE TABLE IF NOT EXISTS postal_codes (
    id                BIGSERIAL    PRIMARY KEY,
    pin_code          VARCHAR(10)  NOT NULL,
    locality_name     VARCHAR(150),
    post_office_name  VARCHAR(150),

    -- Structural FK (normalized hierarchy)
    taluk_id          BIGINT       REFERENCES taluks(id),

    -- Denormalized FKs (NOT NULL — required for fast read-path queries)
    district_id       BIGINT       NOT NULL REFERENCES districts(id),
    state_id          BIGINT       NOT NULL REFERENCES states(id),
    country_id        BIGINT       NOT NULL REFERENCES countries(id),

    is_active         BOOLEAN      NOT NULL DEFAULT TRUE,
    version           BIGINT       NOT NULL DEFAULT 0,
    created_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP,
    created_by        VARCHAR(255),
    updated_by        VARCHAR(255),

    -- Prevents duplicate rows for same pincode + locality + post office
    CONSTRAINT uq_postal_code_locality_po UNIQUE (pin_code, locality_name, post_office_name)
);

COMMENT ON TABLE  postal_codes                   IS 'Master postal code table — leaf of the location hierarchy';
COMMENT ON COLUMN postal_codes.pin_code          IS 'Postal code (VARCHAR(10) to support international formats)';
COMMENT ON COLUMN postal_codes.locality_name     IS 'Village / locality / area name within the pincode';
COMMENT ON COLUMN postal_codes.post_office_name  IS 'India Post office name serving this locality';
COMMENT ON COLUMN postal_codes.district_id       IS 'Denormalized FK — avoids deep joins on read path';
COMMENT ON COLUMN postal_codes.state_id          IS 'Denormalized FK — avoids deep joins on read path';
COMMENT ON COLUMN postal_codes.country_id        IS 'Denormalized FK — avoids deep joins on read path';

-- Primary lookup index — used by pincode search API
CREATE INDEX IF NOT EXISTS idx_postal_pin_code
    ON postal_codes(pin_code);

-- Composite index — covers the most common query pattern: pin + locality
CREATE INDEX IF NOT EXISTS idx_postal_lookup
    ON postal_codes(pin_code, locality_name);

-- Support FK-based joins
CREATE INDEX IF NOT EXISTS idx_postal_state_id    ON postal_codes(state_id);
CREATE INDEX IF NOT EXISTS idx_postal_district_id ON postal_codes(district_id);
CREATE INDEX IF NOT EXISTS idx_postal_country_id  ON postal_codes(country_id);
