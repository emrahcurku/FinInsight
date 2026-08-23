package com.fininsight.common.event.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

/**
 * Spring Data JPA Repository for ProcessedEvent entities (Idempotency check).
 */
@Repository
public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, UUID> {

    void deleteByProcessedAtBefore(Instant before);
}
