package com.eshop.app.subscription.api.response;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Response DTO for Subscription data.
 */
public record SubscriptionResponse(
    Long id,
    Long planId,
    String planName,
    String status,
    BigDecimal price,
    Instant startDate,
    Instant endDate,
    boolean autoRenew
) {}
