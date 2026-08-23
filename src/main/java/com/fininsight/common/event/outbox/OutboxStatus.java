package com.fininsight.common.event.outbox;

/**
 * Status of an Outbox event record in the database.
 */
public enum OutboxStatus {
    PENDING,
    PUBLISHED,
    FAILED
}
