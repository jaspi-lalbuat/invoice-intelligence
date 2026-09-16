package com.jasper.invoice.application;

import com.jasper.invoice.domain.model.InvoiceProcessingJob;
import com.jasper.invoice.infrastructure.persistence.InvoiceProcessingJobJpaRepository;
import com.jasper.invoice.infrastructure.persistence.OutboxEventJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class InvoiceJobCreationServiceIntegrationTest {

    @Autowired
    private InvoiceJobCreationService service;

    @Autowired
    private InvoiceProcessingJobJpaRepository jobRepository;

    @Autowired
    private OutboxEventJpaRepository outboxRepository;

    @BeforeEach
    void cleanDatabase() {
        outboxRepository.deleteAll();
        jobRepository.deleteAll();
    }

    @Test
    void shouldCreateJobAndOutboxEventAtomically() {

        String documentReference =
                "document-" + UUID.randomUUID();

        InvoiceProcessingJob job =
                service.createJob(documentReference);

        assertNotNull(job);

        assertTrue(
                jobRepository.existsById(job.id())
        );

        var events =
                outboxRepository.findUnpublished(100);

        assertEquals(1, events.size());

        var event = events.get(0);

        assertEquals(
                job.id(),
                event.getAggregateId()
        );

        assertEquals(
                "InvoiceProcessingRequested",
                event.getEventType()
        );

        assertNotNull(event.getPayload());
        assertNull(event.getPublishedAt());
    }
}