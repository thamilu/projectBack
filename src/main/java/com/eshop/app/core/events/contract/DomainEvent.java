package com.eshop.app.core.events.contract;

public interface DomainEvent {
    String getCorrelationId();
    long getOccurredAt();
    default String getEventType() {
        return this.getClass().getSimpleName();
    }
}
