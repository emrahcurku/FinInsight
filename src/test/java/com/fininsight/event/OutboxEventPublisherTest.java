package com.fininsight.event;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fininsight.common.event.TransactionEvent;
import com.fininsight.common.event.outbox.OutboxEvent;
import com.fininsight.common.event.outbox.OutboxEventPublisher;
import com.fininsight.common.event.outbox.OutboxEventRepository;
import com.fininsight.common.event.outbox.OutboxStatus;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@ExtendWith(MockitoExtension.class)
public class OutboxEventPublisherTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    private ObjectMapper objectMapper;
    private OutboxEventPublisher outboxEventPublisher;

    @BeforeEach
    public void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        outboxEventPublisher = new OutboxEventPublisher(outboxEventRepository, objectMapper);
    }

    @Test
    @DisplayName("Publishing domain event creates and saves OutboxEvent with PENDING status")
    public void testPublishEventPersistsOutboxEvent() {
        UUID userId = UUID.randomUUID();
        UUID txId = UUID.randomUUID();
        UUID catId = UUID.randomUUID();
        TransactionEvent event = TransactionEvent.created(
                userId, txId, catId, new BigDecimal("100.00"), "EXPENSE", LocalDate.now(), "corr-1"
        );

        outboxEventPublisher.publish(event);

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(captor.capture());

        OutboxEvent saved = captor.getValue();
        assertThat(saved.getId()).isEqualTo(event.getEventId());
        assertThat(saved.getAggregateType()).isEqualTo("TRANSACTION");
        assertThat(saved.getAggregateId()).isEqualTo(txId);
        assertThat(saved.getEventType()).isEqualTo("TransactionCreatedEvent");
        assertThat(saved.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(saved.getRetryCount()).isEqualTo(0);
        assertThat(saved.getPayload()).contains(userId.toString());
    }

    @Test
    @DisplayName("Publishing null event is safely skipped")
    public void testPublishNullEventSkipped() {
        outboxEventPublisher.publish(null);
        verify(outboxEventRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
