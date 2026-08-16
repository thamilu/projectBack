package com.eshop.app.user.shared.domain.enums;

/**
 * Represents the lifecycle states of a {@code DeliveryAgent} within the
 * eShop platform. Covers onboarding, active service, and various
 * suspension/termination scenarios.
 */
public enum DeliveryAgentStatus {

    /** Account created but not yet approved for active delivery operations. */
    PENDING,

    /** Account is active and the agent is available for delivery assignments. */
    ACTIVE,

    /** Account is deactivated, agent cannot receive new orders. */
    INACTIVE,

    /** Account is temporarily suspended (e.g., for policy violation or review). */
    SUSPENDED,

    /** Account has been permanently rejected or terminated. */
    REJECTED
}
