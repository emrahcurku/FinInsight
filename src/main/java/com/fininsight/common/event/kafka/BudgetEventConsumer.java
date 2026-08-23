package com.fininsight.common.event.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fininsight.common.cache.CacheEvictionService;
import com.fininsight.common.event.BudgetEvent;
import com.fininsight.common.event.outbox.ProcessedEvent;
import com.fininsight.common.event.outbox.ProcessedEventRepository;
import com.fininsight.config.CorrelationIdFilter;
import com.fininsight.config.kafka.KafkaTopicNames;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Kafka consumer for budget lifecycle events.
 * Idempotently executes targeted cache eviction for the affected user.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class BudgetEventConsumer {

    private final ObjectMapper objectMapper;
    private final ProcessedEventRepository processedEventRepository;
    private final CacheEvictionService cacheEvictionService;

    @KafkaListener(
            topics = KafkaTopicNames.BUDGET_EVENTS,
            groupId = KafkaTopicNames.CONSUMER_GROUP,
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void consume(String message) {
        BudgetEvent event;
        try {
            event = objectMapper.readValue(message, BudgetEvent.class);
        } catch (JsonProcessingException ex) {
            log.error("Failed to deserialize BudgetEvent payload. Skipping invalid message: {}", ex.getMessage());
            return;
        }

        if (event.getEventId() == null) {
            log.warn("Received BudgetEvent with null eventId. Skipping.");
            return;
        }

        if (processedEventRepository.existsById(event.getEventId())) {
            log.info("Idempotent Consumer: BudgetEvent with eventId '{}' has already been processed. Skipping.",
                    event.getEventId());
            return;
        }

        String correlationId = event.getCorrelationId();
        if (correlationId != null) {
            MDC.put(CorrelationIdFilter.MDC_CORRELATION_ID_KEY, correlationId);
        }

        try {
            log.info("Processing BudgetEvent: eventId={}, action={}, userId={}, aggregateId={}",
                    event.getEventId(), event.getAction(), event.getUserId(), event.getAggregateId());

            cacheEvictionService.evictUserBudgetCaches(event.getUserId());

            processedEventRepository.save(new ProcessedEvent(event.getEventId(), event.getEventType()));
            log.debug("BudgetEvent '{}' processed and marked in processed_events", event.getEventId());
        } finally {
            if (correlationId != null) {
                MDC.remove(CorrelationIdFilter.MDC_CORRELATION_ID_KEY);
            }
        }
    }
}
