package com.fininsight.common.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fininsight.config.kafka.KafkaTopicNames;

import lombok.Getter;
import lombok.ToString;

/**
 * Domain event emitted upon Transaction lifecycle changes (create, update, delete).
 */
@Getter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class TransactionEvent implements DomainEvent {

    private final UUID eventId;
    private final String eventType;
    private final int eventVersion;
    private final Instant occurredAt;
    private final UUID userId;
    private final String correlationId;
    private final UUID transactionId;
    private final UUID categoryId;
    private final BigDecimal amount;
    private final String type;
    private final LocalDate transactionDate;
    private final EventAction action;

    @JsonCreator
    public TransactionEvent(
            @JsonProperty("eventId") UUID eventId,
            @JsonProperty("eventType") String eventType,
            @JsonProperty("eventVersion") int eventVersion,
            @JsonProperty("occurredAt") Instant occurredAt,
            @JsonProperty("userId") UUID userId,
            @JsonProperty("correlationId") String correlationId,
            @JsonProperty("transactionId") UUID transactionId,
            @JsonProperty("categoryId") UUID categoryId,
            @JsonProperty("amount") BigDecimal amount,
            @JsonProperty("type") String type,
            @JsonProperty("transactionDate") LocalDate transactionDate,
            @JsonProperty("action") EventAction action
    ) {
        this.eventId = eventId != null ? eventId : UUID.randomUUID();
        this.eventType = eventType != null ? eventType : getClass().getSimpleName();
        this.eventVersion = eventVersion > 0 ? eventVersion : 1;
        this.occurredAt = occurredAt != null ? occurredAt : Instant.now();
        this.userId = userId;
        this.correlationId = correlationId;
        this.transactionId = transactionId;
        this.categoryId = categoryId;
        this.amount = amount;
        this.type = type;
        this.transactionDate = transactionDate;
        this.action = action;
    }

    public static TransactionEvent created(
            UUID userId,
            UUID transactionId,
            UUID categoryId,
            BigDecimal amount,
            String type,
            LocalDate transactionDate,
            String correlationId
    ) {
        return new TransactionEvent(
                UUID.randomUUID(),
                "TransactionCreatedEvent",
                1,
                Instant.now(),
                userId,
                correlationId,
                transactionId,
                categoryId,
                amount,
                type,
                transactionDate,
                EventAction.CREATED
        );
    }

    public static TransactionEvent updated(
            UUID userId,
            UUID transactionId,
            UUID categoryId,
            BigDecimal amount,
            String type,
            LocalDate transactionDate,
            String correlationId
    ) {
        return new TransactionEvent(
                UUID.randomUUID(),
                "TransactionUpdatedEvent",
                1,
                Instant.now(),
                userId,
                correlationId,
                transactionId,
                categoryId,
                amount,
                type,
                transactionDate,
                EventAction.UPDATED
        );
    }

    public static TransactionEvent deleted(
            UUID userId,
            UUID transactionId,
            UUID categoryId,
            String correlationId
    ) {
        return new TransactionEvent(
                UUID.randomUUID(),
                "TransactionDeletedEvent",
                1,
                Instant.now(),
                userId,
                correlationId,
                transactionId,
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
        return transactionId;
    }

    @Override
    @JsonIgnore
    public String getAggregateType() {
        return "TRANSACTION";
    }

    @Override
    @JsonIgnore
    public String getTopic() {
        return KafkaTopicNames.TRANSACTION_EVENTS;
    }
}
