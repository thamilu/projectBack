package com.eshop.app.dto.location;

/**
 * DTO for master taluk data.
 */
public record TalukDTO(
    Long id,
    String name,
    Long districtId
) {}
