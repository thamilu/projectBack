package com.eshop.app.location.api.response;





/**
 * DTO for master state data.
 */
public record StateDTO(
    Long id,
    String name,
    String stateCode
) {}
