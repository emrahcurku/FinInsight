package com.fininsight.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fininsight.common.event.BudgetEvent;
import com.fininsight.common.event.CategoryEvent;
import com.fininsight.common.event.EventAction;
import com.fininsight.common.event.TransactionEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class EventSerializationTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    public void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    @DisplayName("TransactionEvent JSON serialization and deserialization round-trip maintains precision and types")
    public void testTransactionEventSerializationRoundTrip() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID txId = UUID.randomUUID();
        UUID catId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("4999.9950");
        LocalDate date = LocalDate.of(2026, 8, 23);
        String correlationId = "corr-" + UUID.randomUUID();

        TransactionEvent event = TransactionEvent.created(
                userId, txId, catId, amount, "EXPENSE", date, correlationId
        );

        String json = objectMapper.writeValueAsString(event);
        assertThat(json).doesNotContain("password", "token", "secret", "authorization");

        TransactionEvent deserialized = objectMapper.readValue(json, TransactionEvent.class);

        assertThat(deserialized.getEventId()).isEqualTo(event.getEventId());
        assertThat(deserialized.getUserId()).isEqualTo(userId);
        assertThat(deserialized.getTransactionId()).isEqualTo(txId);
        assertThat(deserialized.getCategoryId()).isEqualTo(catId);
        assertThat(deserialized.getAmount()).isEqualByComparingTo(amount);
        assertThat(deserialized.getType()).isEqualTo("EXPENSE");
        assertThat(deserialized.getTransactionDate()).isEqualTo(date);
        assertThat(deserialized.getAction()).isEqualTo(EventAction.CREATED);
        assertThat(deserialized.getCorrelationId()).isEqualTo(correlationId);
    }

    @Test
    @DisplayName("BudgetEvent JSON serialization and deserialization round-trip")
    public void testBudgetEventSerializationRoundTrip() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID budgetId = UUID.randomUUID();
        UUID catId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("1500.0000");
        String correlationId = "corr-budget-123";

        BudgetEvent event = BudgetEvent.updated(
                userId, budgetId, catId, amount, 8, 2026, correlationId
        );

        String json = objectMapper.writeValueAsString(event);
        BudgetEvent deserialized = objectMapper.readValue(json, BudgetEvent.class);

        assertThat(deserialized.getEventId()).isEqualTo(event.getEventId());
        assertThat(deserialized.getUserId()).isEqualTo(userId);
        assertThat(deserialized.getBudgetId()).isEqualTo(budgetId);
        assertThat(deserialized.getAmount()).isEqualByComparingTo(amount);
        assertThat(deserialized.getMonth()).isEqualTo(8);
        assertThat(deserialized.getYear()).isEqualTo(2026);
        assertThat(deserialized.getAction()).isEqualTo(EventAction.UPDATED);
    }

    @Test
    @DisplayName("CategoryEvent JSON serialization and deserialization round-trip")
    public void testCategoryEventSerializationRoundTrip() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID catId = UUID.randomUUID();
        String correlationId = "corr-cat-456";

        CategoryEvent event = CategoryEvent.created(
                userId, catId, "Technology", "EXPENSE", correlationId
        );

        String json = objectMapper.writeValueAsString(event);
        CategoryEvent deserialized = objectMapper.readValue(json, CategoryEvent.class);

        assertThat(deserialized.getEventId()).isEqualTo(event.getEventId());
        assertThat(deserialized.getUserId()).isEqualTo(userId);
        assertThat(deserialized.getCategoryId()).isEqualTo(catId);
        assertThat(deserialized.getName()).isEqualTo("Technology");
        assertThat(deserialized.getType()).isEqualTo("EXPENSE");
        assertThat(deserialized.getAction()).isEqualTo(EventAction.CREATED);
    }
}
