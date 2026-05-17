package com.eshop.app.core.events.publisher;

import com.eshop.app.core.events.contract.DomainEvent;

public interface DomainEventPublisher {
    void publish(DomainEvent event);
    void publishAll(Iterable<? extends DomainEvent> events);
}
