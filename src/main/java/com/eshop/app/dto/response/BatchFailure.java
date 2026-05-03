package com.eshop.app.dto.response;

/**
 * Details of a batch operation failure.
 */
public record BatchFailure(
    int index,
    String identifier,
    String errorMessage,
    String errorCode
) {}
