package com.eshop.app.location.api.response;





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
