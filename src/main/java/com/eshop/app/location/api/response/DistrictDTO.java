package com.eshop.app.location.api.response;





/**
 * DTO for master district data.
 */
public record DistrictDTO(
    Long id,
    String name,
    Long stateId
) {}
