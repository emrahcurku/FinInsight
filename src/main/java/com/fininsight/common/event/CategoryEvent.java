package com.fininsight.common.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fininsight.config.kafka.KafkaTopicNames;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain event emitted upon Category lifecycle changes (create, update, delete).
 */
@Getter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class CategoryEvent implements DomainEvent {

    private final UUID eventId;
    private final String eventType;
    private final int eventVersion;
    private final Instant occurredAt;
    private final UUID userId;
    private final String correlationId;
    private final UUID categoryId;
    private final String name;
    private final String type;
    private final EventAction action;

    @JsonCreator
    public CategoryEvent(
            @JsonProperty("eventId") UUID eventId,
            @JsonProperty("eventType") String eventType,
            @JsonProperty("eventVersion") int eventVersion,
            @JsonProperty("occurredAt") Instant occurredAt,
            @JsonProperty("userId") UUID userId,
            @JsonProperty("correlationId") String correlationId,
            @JsonProperty("categoryId") UUID categoryId,
            @JsonProperty("name") String name,
            @JsonProperty("type") String type,
            @JsonProperty("action") EventAction action
    ) {
        this.eventId = eventId != null ? eventId : UUID.randomUUID();
        this.eventType = eventType != null ? eventType : getClass().getSimpleName();
        this.eventVersion = eventVersion > 0 ? eventVersion : 1;
        this.occurredAt = occurredAt != null ? occurredAt : Instant.now();
        this.userId = userId;
        this.correlationId = correlationId;
        this.categoryId = categoryId;
        this.name = name;
        this.type = type;
        this.action = action;
    }

    public static CategoryEvent created(
            UUID userId,
            UUID categoryId,
            String name,
            String type,
            String correlationId
    ) {
        return new CategoryEvent(
                UUID.randomUUID(),
                "CategoryCreatedEvent",
                1,
                Instant.now(),
                userId,
                correlationId,
                categoryId,
                name,
                type,
                EventAction.CREATED
        );
    }

    public static CategoryEvent updated(
            UUID userId,
            UUID categoryId,
            String name,
            String type,
            String correlationId
    ) {
        return new CategoryEvent(
                UUID.randomUUID(),
                "CategoryUpdatedEvent",
                1,
                Instant.now(),
                userId,
                correlationId,
                categoryId,
                name,
                type,
                EventAction.UPDATED
        );
    }

    public static CategoryEvent deleted(
            UUID userId,
            UUID categoryId,
            String correlationId
    ) {
        return new CategoryEvent(
                UUID.randomUUID(),
                "CategoryDeletedEvent",
                1,
                Instant.now(),
                userId,
                correlationId,
                categoryId,
                null,
                null,
                EventAction.DELETED
        );
    }

    @Override
    @JsonIgnore
    public UUID getAggregateId() {
        return categoryId;
    }

    @Override
    @JsonIgnore
    public String getAggregateType() {
        return "CATEGORY";
    }

    @Override
    @JsonIgnore
    public String getTopic() {
        return KafkaTopicNames.CATEGORY_EVENTS;
    }
}
