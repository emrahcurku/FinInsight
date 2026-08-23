package com.fininsight.common.event;

/**
 * Domain abstraction for publishing domain events.
 * Decouples domain services from specific transport/storage implementations.
 */
public interface DomainEventPublisher {

    /**
     * Publishes a domain event reliably.
     *
     * @param event the domain event to publish
     */
    void publish(DomainEvent event);
}
