package com.fininsight.common.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fininsight.config.kafka.KafkaTopicNames;

import lombok.Getter;
import lombok.ToString;

/**
 * Domain event emitted upon Budget lifecycle changes (create, update, delete).
 */
@Getter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class BudgetEvent implements DomainEvent {

    private final UUID eventId;
    private final String eventType;
    private final int eventVersion;
    private final Instant occurredAt;
    private final UUID userId;
    private final String correlationId;
    private final UUID budgetId;
    private final UUID categoryId;
    private final BigDecimal amount;
    private final Integer month;
    private final Integer year;
    private final EventAction action;

    @JsonCreator
    public BudgetEvent(
            @JsonProperty("eventId") UUID eventId,
            @JsonProperty("eventType") String eventType,
            @JsonProperty("eventVersion") int eventVersion,
            @JsonProperty("occurredAt") Instant occurredAt,
            @JsonProperty("userId") UUID userId,
            @JsonProperty("correlationId") String correlationId,
            @JsonProperty("budgetId") UUID budgetId,
            @JsonProperty("categoryId") UUID categoryId,
            @JsonProperty("amount") BigDecimal amount,
            @JsonProperty("month") Integer month,
            @JsonProperty("year") Integer year,
            @JsonProperty("action") EventAction action
    ) {
        this.eventId = eventId != null ? eventId : UUID.randomUUID();
        this.eventType = eventType != null ? eventType : getClass().getSimpleName();
        this.eventVersion = eventVersion > 0 ? eventVersion : 1;
        this.occurredAt = occurredAt != null ? occurredAt : Instant.now();
        this.userId = userId;
        this.correlationId = correlationId;
        this.budgetId = budgetId;
        this.categoryId = categoryId;
        this.amount = amount;
        this.month = month;
        this.year = year;
        this.action = action;
    }

    public static BudgetEvent created(
            UUID userId,
            UUID budgetId,
            UUID categoryId,
            BigDecimal amount,
            int month,
            int year,
            String correlationId
    ) {
        return new BudgetEvent(
                UUID.randomUUID(),
                "BudgetCreatedEvent",
                1,
                Instant.now(),
                userId,
                correlationId,
                budgetId,
                categoryId,
                amount,
                month,
                year,
                EventAction.CREATED
        );
    }

    public static BudgetEvent updated(
            UUID userId,
            UUID budgetId,
            UUID categoryId,
            BigDecimal amount,
            int month,
            int year,
            String correlationId
    ) {
        return new BudgetEvent(
                UUID.randomUUID(),
                "BudgetUpdatedEvent",
                1,
                Instant.now(),
                userId,
                correlationId,
                budgetId,
                categoryId,
                amount,
                month,
                year,
                EventAction.UPDATED
        );
    }

    public static BudgetEvent deleted(
            UUID userId,
            UUID budgetId,
            UUID categoryId,
            String correlationId
    ) {
        return new BudgetEvent(
                UUID.randomUUID(),
                "BudgetDeletedEvent",
                1,
                Instant.now(),
                userId,
                correlationId,
                budgetId,
                categoryId,
                null,
                null,
                null,
                EventAction.DELETED
        );
    }

    @Override
    @JsonIgnore
    public UUID getAggregateId() {
        return budgetId;
    }

    @Override
    @JsonIgnore
    public String getAggregateType() {
        return "BUDGET";
    }

    @Override
    @JsonIgnore
    public String getTopic() {
        return KafkaTopicNames.BUDGET_EVENTS;
    }
}
