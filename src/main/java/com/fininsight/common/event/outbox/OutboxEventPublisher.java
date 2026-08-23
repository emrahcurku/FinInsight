package com.fininsight.common.event.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fininsight.common.event.DomainEvent;
import com.fininsight.common.event.DomainEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of DomainEventPublisher utilizing the Transactional Outbox Pattern.
 * Persists domain events into the database within the current active transaction.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventPublisher implements DomainEventPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void publish(DomainEvent event) {
        if (event == null) {
            log.warn("Null domain event provided to OutboxEventPublisher. Skipping.");
            return;
        }

        try {
            String payload = objectMapper.writeValueAsString(event);
            OutboxEvent outboxEvent = new OutboxEvent(
                    event.getEventId(),
                    event.getAggregateType(),
                    event.getAggregateId(),
                    event.getEventType(),
                    payload
            );

            outboxEventRepository.save(outboxEvent);
            log.debug("Persisted outbox event: id={}, type={}, aggregateId={}",
                    event.getEventId(), event.getEventType(), event.getAggregateId());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize domain event: id={}, type={}", event.getEventId(), event.getEventType(), e);
            throw new IllegalStateException("Failed to serialize domain event to JSON", e);
        }
    }
}
