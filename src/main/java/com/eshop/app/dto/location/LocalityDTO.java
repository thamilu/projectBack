package com.eshop.app.dto.location;

/**
 * Represents a single locality / post-office pair within a pincode.
 *
 * @param locality    Village or area name (may be null for unnamed areas).
 * @param postOffice  India Post office name serving the locality.
 */
public record LocalityDTO(
        String locality,
        String postOffice
) {}
