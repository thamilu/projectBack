package com.eshop.app.core.kernel;

import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Transient;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.springframework.data.domain.AfterDomainEventPublication;
import org.springframework.data.domain.DomainEvents;

/**
 * Base class for DDD Aggregate Roots.
 *
 * <p>Provides domain event registration following the Spring Data domain events pattern. Events are
 * published automatically when the aggregate is saved via {@code CrudRepository.save()}.
 *
 * <p>Uses {@link List} instead of {@link java.util.Set} to preserve event publication order —
 * critical for event sourcing and eventual consistency guarantees.
 *
 * <h3>Usage:</h3>
 *
 * <pre>{@code
 * public class Order extends AggregateRoot {
 *     public void ship(String trackingNumber) {
 *         this.status = OrderStatus.SHIPPED;
 *         registerEvent(new OrderShippedEvent(this.getId(), trackingNumber));
 *     }
 * }
 * }</pre>
 */
@MappedSuperclass
public abstract class AggregateRoot extends BaseEntity {

    /**
     * Ordered domain event store. {@code @Transient} prevents JPA serialization contamination.
     * {@code final} ensures the list reference is never replaced.
     */
    @Transient private final List<Object> domainEvents = new ArrayList<>();

    /**
     * Publishes all registered events via Spring Data infrastructure. Called by {@link
     * org.springframework.data.repository.CrudRepository#save}.
     *
     * @return unmodifiable view of pending events
     */
    @DomainEvents
    protected Collection<Object> domainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    /**
     * Clears events post-publication. Invoked automatically by Spring Data after event dispatch.
     */
    @AfterDomainEventPublication
    protected void clearDomainEvents() {
        domainEvents.clear();
    }

    /**
     * Registers a domain event for deferred publication.
     *
     * @param event the domain event (must not be null)
     * @throws com.eshop.app.core.exception.business.DomainValidationException if event is null
     */
    protected final void registerEvent(Object event) {
        DomainGuard.requireNotNull(event, "domainEvent");
        domainEvents.add(event);
    }

    /**
     * Returns count of pending domain events. Useful for testing and diagnostics.
     *
     * @return number of events awaiting publication
     */
    public final int pendingEventCount() {
        return domainEvents.size();
    }
}
