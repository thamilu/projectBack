package com.eshop.app.core.infrastructure.config.security.oauth;

import lombok.Value;

/**
 * Event published when user identity synchronization fails permanently after all retry attempts.
 * Supports operational alerting (PagerDuty, Slack, etc.).
 */
@Value
public class UserSyncPermanentFailureEvent {
    String keycloakId;
    String errorMessage;
}
