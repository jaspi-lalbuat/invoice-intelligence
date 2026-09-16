package com.jasper.invoice.infrastructure.persistence;

import com.jasper.invoice.domain.outbox.OutboxEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class JpaOutboxEventRepositoryIntegrationTest {

    @Autowired
    private JpaOutboxEventRepository outboxRepository;

    @Autowired
    private OutboxEventJpaRepository jpaRepository;

    @BeforeEach
    void cleanOutbox() {
        jpaRepository.deleteAll();
    }

    @Test
    void shouldSaveAndFindUnpublishedEvent() {
        OutboxEvent event = new OutboxEvent(
                UUID.randomUUID(),
                "InvoiceProcessingRequested",
                UUID.randomUUID(),
                "{\"jobId\":\"123\"}",
                Instant.now()
        );

        OutboxEvent saved = outboxRepository.save(event);

        List<OutboxEvent> unpublished =
                outboxRepository.findUnpublished(10);

        assertThat(saved.id()).isEqualTo(event.id());
        assertThat(unpublished)
                .extracting(OutboxEvent::id)
                .contains(event.id());
    }

    @Test
    void shouldMarkEventAsPublished() {
        OutboxEvent event = new OutboxEvent(
                UUID.randomUUID(),
                "InvoiceProcessingRequested",
                UUID.randomUUID(),
                "{\"jobId\":\"123\"}",
                Instant.now()
        );

        outboxRepository.save(event);

        outboxRepository.markPublished(event);

        List<OutboxEvent> unpublished =
                outboxRepository.findUnpublished(10);

        assertThat(unpublished)
                .extracting(OutboxEvent::id)
                .doesNotContain(event.id());

        OutboxEventEntity entity =
                jpaRepository.findById(event.id()).orElseThrow();

        assertThat(entity.getPublishedAt()).isNotNull();
    }

    @Test
    void shouldReturnOnlyUnpublishedEvents() {
        OutboxEvent unpublished = new OutboxEvent(
                UUID.randomUUID(),
                "InvoiceProcessingRequested",
                UUID.randomUUID(),
                "{\"jobId\":\"1\"}",
                Instant.now()
        );

        OutboxEvent published = new OutboxEvent(
                UUID.randomUUID(),
                "InvoiceProcessingRequested",
                UUID.randomUUID(),
                "{\"jobId\":\"2\"}",
                Instant.now()
        );

        outboxRepository.save(unpublished);
        outboxRepository.save(published);
        outboxRepository.markPublished(published);

        List<OutboxEvent> events =
                outboxRepository.findUnpublished(10);

        assertThat(events)
                .extracting(OutboxEvent::id)
                .contains(unpublished.id())
                .doesNotContain(published.id());
    }

    @Test
    void shouldRespectBatchLimit() {
        for (int i = 0; i < 5; i++) {
            outboxRepository.save(
                    new OutboxEvent(
                            UUID.randomUUID(),
                            "InvoiceProcessingRequested",
                            UUID.randomUUID(),
                            "{\"jobId\":\"" + i + "\"}",
                            Instant.now()
                    )
            );
        }

        List<OutboxEvent> events =
                outboxRepository.findUnpublished(2);

        assertThat(events).hasSize(2);
    }
}