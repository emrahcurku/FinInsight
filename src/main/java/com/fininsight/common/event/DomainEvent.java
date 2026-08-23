package com.fininsight.common.event;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.UUID;

/**
 * Base contract for all domain events across FinInsight modules.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public interface DomainEvent {

    UUID getEventId();

    String getEventType();

    int getEventVersion();

    Instant getOccurredAt();

    UUID getUserId();

    String getCorrelationId();

    @JsonIgnore
    UUID getAggregateId();

    @JsonIgnore
    String getAggregateType();

    @JsonIgnore
    String getTopic();

    @JsonIgnore
    default String getPartitionKey() {
        return getUserId() != null ? getUserId().toString() : (getAggregateId() != null ? getAggregateId().toString() : "");
    }
}
