package com.fininsight.common.event.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fininsight.common.event.kafka.KafkaEventPublisher;
import com.fininsight.config.kafka.KafkaTopicNames;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Background poller for the Transactional Outbox.
 * Fetches PENDING events from the database and publishes them to the corresponding Kafka topics.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "application.outbox.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxScheduler {

    private final OutboxEventRepository outboxEventRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final KafkaEventPublisher kafkaEventPublisher;
    private final ObjectMapper objectMapper;

    @Value("${application.outbox.batch-size:50}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${application.outbox.polling-interval-ms:1000}")
    @Transactional
    public void processOutboxEvents() {
        List<OutboxEvent> pendingEvents = outboxEventRepository.findByStatusOrderByCreatedAtAsc(
                OutboxStatus.PENDING, PageRequest.of(0, batchSize)
        );

        if (pendingEvents.isEmpty()) {
            return;
        }

        log.debug("Processing {} pending outbox event(s)", pendingEvents.size());

        for (OutboxEvent outboxEvent : pendingEvents) {
            String topic = resolveTopic(outboxEvent.getAggregateType());
            if (topic == null) {
                log.error("Unknown aggregate type '{}' for outbox event id '{}'",
                        outboxEvent.getAggregateType(), outboxEvent.getId());
                outboxEvent.markFailed("Unknown aggregate type: " + outboxEvent.getAggregateType());
                outboxEventRepository.save(outboxEvent);
                continue;
            }

            String partitionKey = resolvePartitionKey(outboxEvent.getPayload(), outboxEvent.getAggregateId().toString());

            try {
                kafkaEventPublisher.send(topic, partitionKey, outboxEvent.getPayload())
                        .get(5, TimeUnit.SECONDS);

                outboxEvent.markPublished();
                outboxEventRepository.save(outboxEvent);
                log.debug("Successfully published outbox event id '{}' to topic '{}'", outboxEvent.getId(), topic);
            } catch (InterruptedException ex) {
                log.warn("Publishing outbox event id '{}' was interrupted: {}",
                        outboxEvent.getId(), ex.getMessage());
                Thread.currentThread().interrupt();
                outboxEvent.markFailed("Interrupted: " + ex.getMessage());
                outboxEventRepository.save(outboxEvent);
            } catch (ExecutionException | TimeoutException ex) {
                log.warn("Failed to publish outbox event id '{}' to Kafka: {}",
                        outboxEvent.getId(), ex.getMessage());
                outboxEvent.markFailed(ex.getMessage());
                outboxEventRepository.save(outboxEvent);
            }
        }
    }

    @Scheduled(cron = "${application.outbox.cleanup-cron:0 0 3 * * *}")
    @Transactional
    public void cleanupOldEvents() {
        Instant retentionCutoff = Instant.now().minus(7, ChronoUnit.DAYS);
        try {
            outboxEventRepository.deleteByStatusAndPublishedAtBefore(OutboxStatus.PUBLISHED, retentionCutoff);
            processedEventRepository.deleteByProcessedAtBefore(retentionCutoff);
            log.info("Cleaned up published outbox and processed events older than {}", retentionCutoff);
        } catch (Exception ex) {
            log.warn("Failed to clean up old outbox/processed events: {}", ex.getMessage());
        }
    }

    private String resolveTopic(String aggregateType) {
        if ("TRANSACTION".equalsIgnoreCase(aggregateType)) {
            return KafkaTopicNames.TRANSACTION_EVENTS;
        } else if ("BUDGET".equalsIgnoreCase(aggregateType)) {
            return KafkaTopicNames.BUDGET_EVENTS;
        } else if ("CATEGORY".equalsIgnoreCase(aggregateType)) {
            return KafkaTopicNames.CATEGORY_EVENTS;
        }
        return null;
    }

    private String resolvePartitionKey(String payload, String fallbackKey) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            if (node.hasNonNull("userId")) {
                return node.get("userId").asText();
            }
        } catch (JsonProcessingException ignored) {
            // fallback to aggregateId
        }
        return fallbackKey;
    }
}
