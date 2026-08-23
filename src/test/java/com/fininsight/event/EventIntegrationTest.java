package com.fininsight.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fininsight.common.cache.CacheEvictionService;
import com.fininsight.common.event.TransactionEvent;
import com.fininsight.common.event.kafka.KafkaEventPublisher;
import com.fininsight.common.event.kafka.TransactionEventConsumer;
import com.fininsight.common.event.outbox.OutboxEvent;
import com.fininsight.common.event.outbox.OutboxEventPublisher;
import com.fininsight.common.event.outbox.OutboxEventRepository;
import com.fininsight.common.event.outbox.OutboxScheduler;
import com.fininsight.common.event.outbox.OutboxStatus;
import com.fininsight.common.event.outbox.ProcessedEventRepository;
import com.fininsight.config.kafka.KafkaTopicNames;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class EventIntegrationTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private ProcessedEventRepository processedEventRepository;

    @Mock
    private KafkaEventPublisher kafkaEventPublisher;

    @Mock
    private CacheEvictionService cacheEvictionService;

    private ObjectMapper objectMapper;
    private OutboxEventPublisher outboxEventPublisher;
    private OutboxScheduler outboxScheduler;
    private TransactionEventConsumer transactionEventConsumer;

    @BeforeEach
    public void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        outboxEventPublisher = new OutboxEventPublisher(outboxEventRepository, objectMapper);
        outboxScheduler = new OutboxScheduler(
                outboxEventRepository,
                processedEventRepository,
                kafkaEventPublisher,
                objectMapper
        );
        ReflectionTestUtils.setField(outboxScheduler, "batchSize", 50);

        transactionEventConsumer = new TransactionEventConsumer(
                objectMapper,
                processedEventRepository,
                cacheEvictionService
        );
    }

    @Test
    @DisplayName("End-to-end event pipeline: Publish -> Outbox -> Scheduler -> Kafka -> Consumer -> Cache Invalidation")
    public void testEndToEndEventDrivenCacheInvalidation() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID txId = UUID.randomUUID();
        UUID catId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("1200.0000");

        TransactionEvent domainEvent = TransactionEvent.created(
                userId, txId, catId, amount, "INCOME", LocalDate.now(), "corr-e2e-1"
        );

        // 1. Publish domain event to Outbox
        outboxEventPublisher.publish(domainEvent);

        ArgumentCaptor<OutboxEvent> outboxCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(outboxCaptor.capture());
        OutboxEvent outboxRecord = outboxCaptor.getValue();

        assertThat(outboxRecord.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(outboxRecord.getAggregateId()).isEqualTo(txId);

        // 2. Outbox Scheduler polls pending events and publishes to Kafka
        when(outboxEventRepository.findByStatusOrderByCreatedAtAsc(eq(OutboxStatus.PENDING), any(PageRequest.class)))
                .thenReturn(List.of(outboxRecord));

        when(kafkaEventPublisher.send(eq(KafkaTopicNames.TRANSACTION_EVENTS), eq(userId.toString()), eq(outboxRecord.getPayload())))
                .thenReturn(CompletableFuture.completedFuture(null));

        outboxScheduler.processOutboxEvents();

        assertThat(outboxRecord.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);

        // 3. Consumer receives message and executes idempotent cache invalidation
        when(processedEventRepository.existsById(domainEvent.getEventId())).thenReturn(false);

        transactionEventConsumer.consume(outboxRecord.getPayload());

        // 4. Verify targeted cache eviction was invoked for userId
        verify(cacheEvictionService).evictUserTransactionCaches(userId);
        verify(processedEventRepository).save(any());
    }
}
