package com.eshop.app.core.api.response;





/**
 * Details of a batch operation failure.
 */
public record BatchFailure(
    int index,
    String identifier,
    String errorMessage,
    String errorCode
) {}


