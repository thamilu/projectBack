package com.eshop.app.dto.location;

/**
 * DTO for master district data.
 */
public record DistrictDTO(
    Long id,
    String name,
    Long stateId
) {}
