package com.eshop.app.dto.location;

/**
 * DTO for master pincode data.
 */
public record PincodeDTO(
    Long id,
    String code,
    Long districtId,
    String taluk,
    String officeName
) {}
