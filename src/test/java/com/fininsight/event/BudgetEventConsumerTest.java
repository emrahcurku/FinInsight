package com.fininsight.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fininsight.common.cache.CacheEvictionService;
import com.fininsight.common.event.BudgetEvent;
import com.fininsight.common.event.kafka.BudgetEventConsumer;
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
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class BudgetEventConsumerTest {

    @Mock
    private ProcessedEventRepository processedEventRepository;

    @Mock
    private CacheEvictionService cacheEvictionService;

    @Spy
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @InjectMocks
    private BudgetEventConsumer consumer;

    @BeforeEach
    public void setUp() {
    }

    @Test
    @DisplayName("BudgetEventConsumer evicts user budget cache and records event as processed")
    public void testConsumeBudgetEventSuccess() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID budgetId = UUID.randomUUID();
        UUID catId = UUID.randomUUID();
        BudgetEvent event = BudgetEvent.created(
                userId, budgetId, catId, new BigDecimal("800.00"), 8, 2026, "corr-b-1"
        );

        String message = objectMapper.writeValueAsString(event);
        when(processedEventRepository.existsById(event.getEventId())).thenReturn(false);

        consumer.consume(message);

        verify(cacheEvictionService).evictUserBudgetCaches(userId);
        verify(processedEventRepository).save(any(ProcessedEvent.class));
    }

    @Test
    @DisplayName("BudgetEventConsumer is idempotent and skips already processed events")
    public void testConsumeBudgetIdempotent() throws Exception {
        UUID userId = UUID.randomUUID();
        BudgetEvent event = BudgetEvent.deleted(userId, UUID.randomUUID(), UUID.randomUUID(), "corr-b-2");

        String message = objectMapper.writeValueAsString(event);
        when(processedEventRepository.existsById(event.getEventId())).thenReturn(true);

        consumer.consume(message);

        verify(cacheEvictionService, never()).evictUserBudgetCaches(any());
        verify(processedEventRepository, never()).save(any());
    }
}
