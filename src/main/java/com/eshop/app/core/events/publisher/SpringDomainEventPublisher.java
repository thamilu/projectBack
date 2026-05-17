package com.eshop.app.core.events.publisher;

import com.eshop.app.core.events.contract.DomainEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SpringDomainEventPublisher implements DomainEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public SpringDomainEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public void publish(DomainEvent event) {
        log.debug("Publishing domain event: type={}, correlationId={}",
            event.getEventType(), event.getCorrelationId());
        applicationEventPublisher.publishEvent(event);
    }

    @Override
    public void publishAll(Iterable<? extends DomainEvent> events) {
        events.forEach(this::publish);
    }
}
