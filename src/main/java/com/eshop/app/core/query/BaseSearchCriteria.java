package com.eshop.app.core.query;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * [HARDEN] Reusable base search criteria — standardized filter contract.
 *
 * All module-specific search criteria should extend this class.
 * Provides the common filter fields shared across all list APIs:
 * - Keyword search
 * - Date range filtering
 * - Active/inactive filtering
 *
 * Additional module-specific filters are added by extending this class.
 * Never add business logic to this class.
 */
public abstract class BaseSearchCriteria {

    /**
     * Full-text keyword search term. Applied to name, description, code as
     * applicable.
     */
    private String keyword;

    /**
     * Filter by active status. Null = return all, true = active only, false =
     * inactive only.
     */
    private Boolean active;

    /** Optional additional filters as key-value pairs for flexible querying. */
    private Map<String, Object> additionalFilters = new HashMap<>();

    // ==================== Constructors ====================

    protected BaseSearchCriteria() {
    }

    // ==================== Getters/Setters ====================

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Map<String, Object> getAdditionalFilters() {
        return Collections.unmodifiableMap(additionalFilters);
    }

    public void addFilter(String key, Object value) {
        this.additionalFilters.put(key, value);
    }

    /**
     * Returns true if any filter has been specified.
     * Useful for deciding whether to apply the "active only" default.
     */
    public boolean hasFilters() {
        return keyword != null || active != null || !additionalFilters.isEmpty();
    }
}
