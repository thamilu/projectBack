package com.eshop.app.location.api.response;





/**
 * DTO for master taluk data.
 */
public record TalukDTO(
    Long id,
    String name,
    Long districtId
) {}
