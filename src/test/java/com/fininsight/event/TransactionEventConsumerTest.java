package com.fininsight.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fininsight.common.cache.CacheEvictionService;
import com.fininsight.common.event.TransactionEvent;
import com.fininsight.common.event.kafka.TransactionEventConsumer;
import com.fininsight.common.event.outbox.ProcessedEvent;
import com.fininsight.common.event.outbox.ProcessedEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TransactionEventConsumerTest {

    @Mock
    private ProcessedEventRepository processedEventRepository;

    @Mock
    private CacheEvictionService cacheEvictionService;

    @Spy
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @InjectMocks
    private TransactionEventConsumer consumer;

    @BeforeEach
    public void setUp() {
    }

    @Test
    @DisplayName("TransactionEventConsumer evicts user cache and records event as processed")
    public void testConsumeTransactionEventSuccess() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID txId = UUID.randomUUID();
        UUID catId = UUID.randomUUID();
        TransactionEvent event = TransactionEvent.created(
                userId, txId, catId, new BigDecimal("250.00"), "EXPENSE", LocalDate.now(), "corr-tx-1"
        );

        String message = objectMapper.writeValueAsString(event);

        when(processedEventRepository.existsById(event.getEventId())).thenReturn(false);

        consumer.consume(message);

        verify(cacheEvictionService).evictUserTransactionCaches(userId);
        verify(processedEventRepository).save(any(ProcessedEvent.class));
    }

    @Test
    @DisplayName("TransactionEventConsumer is idempotent and skips already processed events")
    public void testConsumeTransactionEventIdempotent() throws Exception {
        UUID userId = UUID.randomUUID();
        TransactionEvent event = TransactionEvent.created(
                userId, UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("100.00"), "INCOME", LocalDate.now(), "corr-tx-2"
        );

        String message = objectMapper.writeValueAsString(event);

        when(processedEventRepository.existsById(event.getEventId())).thenReturn(true);

        consumer.consume(message);

        verify(cacheEvictionService, never()).evictUserTransactionCaches(any());
        verify(processedEventRepository, never()).save(any());
    }

    @Test
    @DisplayName("TransactionEventConsumer isolates cache eviction to event user (User A does not evict User B)")
    public void testUserIsolationInCacheEviction() throws Exception {
        UUID userA = UUID.randomUUID();
        UUID userB = UUID.randomUUID();

        TransactionEvent eventA = TransactionEvent.created(
                userA, UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("50.00"), "EXPENSE", LocalDate.now(), "corr-iso"
        );

        String messageA = objectMapper.writeValueAsString(eventA);
        when(processedEventRepository.existsById(eventA.getEventId())).thenReturn(false);

        consumer.consume(messageA);

        verify(cacheEvictionService).evictUserTransactionCaches(eq(userA));
        verify(cacheEvictionService, never()).evictUserTransactionCaches(eq(userB));
    }

    @Test
    @DisplayName("TransactionEventConsumer handles malformed JSON message gracefully without crashing")
    public void testMalformedMessageHandled() {
        consumer.consume("invalid-non-json-message");

        verify(cacheEvictionService, never()).evictUserTransactionCaches(any());
        verify(processedEventRepository, never()).save(any());
    }
}
