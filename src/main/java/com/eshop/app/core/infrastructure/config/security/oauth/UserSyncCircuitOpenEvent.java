package com.eshop.app.core.infrastructure.config.security.oauth;

import lombok.Value;

/** Event published when the user identity synchronization circuit breaker opens. */
@Value
public class UserSyncCircuitOpenEvent {
    String keycloakId;
}
