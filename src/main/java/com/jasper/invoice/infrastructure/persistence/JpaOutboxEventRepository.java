package com.jasper.invoice.infrastructure.persistence;

import com.jasper.invoice.domain.outbox.OutboxEvent;
import com.jasper.invoice.domain.repository.OutboxEventRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Repository
public class JpaOutboxEventRepository implements OutboxEventRepository {

    private final OutboxEventJpaRepository repository;

    public JpaOutboxEventRepository(
            OutboxEventJpaRepository repository
    ) {
        this.repository = repository;
    }

    @Override
    public OutboxEvent save(OutboxEvent event) {
        OutboxEventEntity entity = new OutboxEventEntity();

        entity.setId(event.id());
        entity.setEventType(event.eventType());
        entity.setAggregateId(event.aggregateId());
        entity.setPayload(event.payload());
        entity.setCreatedAt(event.createdAt());

        OutboxEventEntity saved = repository.save(entity);

        return toDomain(saved);
    }

    @Override
    public List<OutboxEvent> findUnpublished(int limit) {
        return repository.findUnpublished(limit)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Transactional
    @Override
    public void markPublished(OutboxEvent event) {
        repository.markPublished(
                event.id(),
                Instant.now()
        );
    }

    private OutboxEvent toDomain(OutboxEventEntity entity) {
        return new OutboxEvent(
                entity.getId(),
                entity.getEventType(),
                entity.getAggregateId(),
                entity.getPayload(),
                entity.getCreatedAt()
        );
    }
}