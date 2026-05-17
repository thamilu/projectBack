package com.eshop.app.subscription.api.request;

import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for creating a new subscription.
 */
public class CreateSubscriptionRequest {

    @NotNull(message = "Plan ID is required")
    private Long planId;

    private String promoCode;

    public Long getPlanId() { return planId; }
    public void setPlanId(Long planId) { this.planId = planId; }

    public String getPromoCode() { return promoCode; }
    public void setPromoCode(String promoCode) { this.promoCode = promoCode; }
}
