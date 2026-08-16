package com.eshop.app.user.domain.event;

import com.eshop.app.core.events.contract.DomainEvent;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.UUID;

/** Immutable domain event published when a user is deactivated. */
public final class UserDeactivatedEvent implements DomainEvent {

    public static final String EVENT_TYPE = "user.deactivated";

    @JsonProperty("eventId")
    private final String eventId;

    @JsonProperty("eventType")
    private final String eventType;

    @JsonProperty("occurredAt")
    private final Instant occurredAt;

    @JsonProperty("correlationId")
    private final String correlationId;

    @JsonProperty("keycloakId")
    private final String keycloakId;

    @JsonCreator
    public UserDeactivatedEvent(
            @JsonProperty("eventId") String eventId,
            @JsonProperty("eventType") String eventType,
            @JsonProperty("occurredAt") Instant occurredAt,
            @JsonProperty("correlationId") String correlationId,
            @JsonProperty("keycloakId") String keycloakId) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.occurredAt = occurredAt;
        this.correlationId = correlationId;
        this.keycloakId = keycloakId;
    }

    public static UserDeactivatedEvent of(String keycloakId) {
        String correlation = org.slf4j.MDC.get("traceId");
        if (correlation == null || correlation.isBlank()) {
            correlation = UUID.randomUUID().toString();
        }
        return new UserDeactivatedEvent(
                UUID.randomUUID().toString(), EVENT_TYPE, Instant.now(), correlation, keycloakId);
    }

    @Override
    public String getEventType() {
        return eventType;
    }

    @Override
    public long getOccurredAt() {
        return occurredAt.toEpochMilli();
    }

    @Override
    public String getCorrelationId() {
        return correlationId;
    }

    public String getEventId() {
        return eventId;
    }

    public Instant getOccurredAtInstant() {
        return occurredAt;
    }

    public String getKeycloakId() {
        return keycloakId;
    }
}
