package com.jasper.invoice.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OutboxEventJpaRepository
        extends JpaRepository<OutboxEventEntity, UUID> {

    @Query(value = """
    SELECT *
    FROM outbox_events
    WHERE published_at IS NULL
    ORDER BY created_at
    LIMIT :limit
    """, nativeQuery = true)
    List<OutboxEventEntity> findUnpublished(
            @Param("limit") int limit
    );

    @Modifying(
            flushAutomatically = true,
            clearAutomatically = true
    )
    @Query("""
    UPDATE OutboxEventEntity e
    SET e.publishedAt = :publishedAt
    WHERE e.id = :id
      AND e.publishedAt IS NULL
""")
    int markPublished(
            @Param("id") UUID id,
            @Param("publishedAt") Instant publishedAt
    );
}