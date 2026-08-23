package com.fininsight.event;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import com.fininsight.common.event.kafka.KafkaEventPublisher;
import com.fininsight.common.event.outbox.OutboxEvent;
import com.fininsight.common.event.outbox.OutboxEventRepository;
import com.fininsight.common.event.outbox.OutboxScheduler;
import com.fininsight.common.event.outbox.OutboxStatus;
import com.fininsight.common.event.outbox.ProcessedEventRepository;
import com.fininsight.config.kafka.KafkaTopicNames;

@ExtendWith(MockitoExtension.class)
public class OutboxSchedulerTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private ProcessedEventRepository processedEventRepository;

    @Mock
    private KafkaEventPublisher kafkaEventPublisher;


    @InjectMocks
    private OutboxScheduler outboxScheduler;

    @BeforeEach
    public void setUp() {
        ReflectionTestUtils.setField(outboxScheduler, "batchSize", 50);
    }

    @Test
    @DisplayName("Poller processes pending outbox event and marks as PUBLISHED on success")
    public void testProcessOutboxEventsSuccess() {
        UUID eventId = UUID.randomUUID();
        UUID txId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String payload = "{\"eventId\":\"" + eventId + "\",\"userId\":\"" + userId + "\"}";

        OutboxEvent event = new OutboxEvent(eventId, "TRANSACTION", txId, "TransactionCreatedEvent", payload);

        when(outboxEventRepository.findByStatusOrderByCreatedAtAsc(eq(OutboxStatus.PENDING), any(Pageable.class)))
                .thenReturn(List.of(event));

        when(kafkaEventPublisher.send(eq(KafkaTopicNames.TRANSACTION_EVENTS), eq(userId.toString()), eq(payload)))
                .thenReturn(CompletableFuture.completedFuture(null));

        outboxScheduler.processOutboxEvents();

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(event.getPublishedAt()).isNotNull();
        verify(outboxEventRepository).save(event);
    }

    @Test
    @DisplayName("Poller marks event with error on Kafka publishing failure")
    public void testProcessOutboxEventsFailure() {
        UUID eventId = UUID.randomUUID();
        UUID txId = UUID.randomUUID();
        String payload = "{\"eventId\":\"" + eventId + "\"}";

        OutboxEvent event = new OutboxEvent(eventId, "TRANSACTION", txId, "TransactionCreatedEvent", payload);

        when(outboxEventRepository.findByStatusOrderByCreatedAtAsc(eq(OutboxStatus.PENDING), any(Pageable.class)))
                .thenReturn(List.of(event));

        CompletableFuture<org.springframework.kafka.support.SendResult<String, String>> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Broker unavailable"));

        when(kafkaEventPublisher.send(eq(KafkaTopicNames.TRANSACTION_EVENTS), eq(txId.toString()), eq(payload)))
                .thenReturn(failedFuture);

        outboxScheduler.processOutboxEvents();

        assertThat(event.getRetryCount()).isEqualTo(1);
        assertThat(event.getErrorMessage()).contains("Broker unavailable");
        verify(outboxEventRepository).save(event);
    }

    @Test
    @DisplayName("Poller handles empty pending list gracefully")
    public void testProcessOutboxEventsEmpty() {
        when(outboxEventRepository.findByStatusOrderByCreatedAtAsc(eq(OutboxStatus.PENDING), any(Pageable.class)))
                .thenReturn(Collections.emptyList());

        outboxScheduler.processOutboxEvents();

        verify(outboxEventRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    @DisplayName("Cleanup task invokes repository deletions")
    public void testCleanupOldEvents() {
        outboxScheduler.cleanupOldEvents();
        verify(outboxEventRepository).deleteByStatusAndPublishedAtBefore(eq(OutboxStatus.PUBLISHED), any());
        verify(processedEventRepository).deleteByProcessedAtBefore(any());
    }
}
