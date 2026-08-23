package com.fininsight.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fininsight.common.cache.CacheEvictionService;
import com.fininsight.common.event.CategoryEvent;
import com.fininsight.common.event.kafka.CategoryEventConsumer;
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

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CategoryEventConsumerTest {

    @Mock
    private ProcessedEventRepository processedEventRepository;

    @Mock
    private CacheEvictionService cacheEvictionService;

    @Spy
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @InjectMocks
    private CategoryEventConsumer consumer;

    @BeforeEach
    public void setUp() {
    }

    @Test
    @DisplayName("CategoryEventConsumer evicts user category cache and records event as processed")
    public void testConsumeCategoryEventSuccess() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID catId = UUID.randomUUID();
        CategoryEvent event = CategoryEvent.created(
                userId, catId, "Gym & Fitness", "EXPENSE", "corr-cat-1"
        );

        String message = objectMapper.writeValueAsString(event);
        when(processedEventRepository.existsById(event.getEventId())).thenReturn(false);

        consumer.consume(message);

        verify(cacheEvictionService).evictUserCategoryCaches(userId);
        verify(processedEventRepository).save(any(ProcessedEvent.class));
    }

    @Test
    @DisplayName("CategoryEventConsumer is idempotent and skips already processed events")
    public void testConsumeCategoryIdempotent() throws Exception {
        UUID userId = UUID.randomUUID();
        CategoryEvent event = CategoryEvent.deleted(userId, UUID.randomUUID(), "corr-cat-2");

        String message = objectMapper.writeValueAsString(event);
        when(processedEventRepository.existsById(event.getEventId())).thenReturn(true);

        consumer.consume(message);

        verify(cacheEvictionService, never()).evictUserCategoryCaches(any());
        verify(processedEventRepository, never()).save(any());
    }
}
