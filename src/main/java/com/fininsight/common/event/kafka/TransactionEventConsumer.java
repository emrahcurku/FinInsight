package com.fininsight.common.event.kafka;

import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fininsight.common.cache.CacheEvictionService;
import com.fininsight.common.event.TransactionEvent;
import com.fininsight.common.event.outbox.ProcessedEvent;
import com.fininsight.common.event.outbox.ProcessedEventRepository;
import com.fininsight.config.CorrelationIdFilter;
import com.fininsight.config.kafka.KafkaTopicNames;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Kafka consumer for transaction lifecycle events.
 * Idempotently executes targeted cache eviction for the affected user.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class TransactionEventConsumer {

    private final ObjectMapper objectMapper;
    private final ProcessedEventRepository processedEventRepository;
    private final CacheEvictionService cacheEvictionService;

    @KafkaListener(
            topics = KafkaTopicNames.TRANSACTION_EVENTS,
            groupId = KafkaTopicNames.CONSUMER_GROUP,
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void consume(String message) {
        TransactionEvent event;
        try {
            event = objectMapper.readValue(message, TransactionEvent.class);
        } catch (JsonProcessingException ex) {
            log.error("Failed to deserialize TransactionEvent payload. Skipping invalid message: {}", ex.getMessage());
            return;
        }

        if (event.getEventId() == null) {
            log.warn("Received TransactionEvent with null eventId. Skipping.");
            return;
        }

        if (processedEventRepository.existsById(event.getEventId())) {
            log.info("Idempotent Consumer: TransactionEvent with eventId '{}' has already been processed. Skipping.",
                    event.getEventId());
            return;
        }

        String correlationId = event.getCorrelationId();
        if (correlationId != null) {
            MDC.put(CorrelationIdFilter.MDC_CORRELATION_ID_KEY, correlationId);
        }

        try {
            log.info("Processing TransactionEvent: eventId={}, action={}, userId={}, aggregateId={}",
                    event.getEventId(), event.getAction(), event.getUserId(), event.getAggregateId());

            cacheEvictionService.evictUserTransactionCaches(event.getUserId());

            processedEventRepository.save(new ProcessedEvent(event.getEventId(), event.getEventType()));
            log.debug("TransactionEvent '{}' processed and marked in processed_events", event.getEventId());
        } finally {
            if (correlationId != null) {
                MDC.remove(CorrelationIdFilter.MDC_CORRELATION_ID_KEY);
            }
        }
    }
}
