package com.eshop.app.core.infrastructure.config.security.oauth;

import java.time.Instant;
import java.util.Set;
import lombok.Builder;
import lombok.Value;

/** POJO Event published to trigger asynchronous user identity and role synchronization. */
@Value
@Builder
public class UserIdentitySyncEvent {
    String keycloakId;
    String email;
    String firstName;
    String lastName;
    String phone;
    Boolean emailVerified;
    Set<String> roles;
    Instant timestamp;
    java.util.UUID eventId;
    String correlationId;
}
