package com.jasper.invoice.application;

import com.jasper.invoice.domain.repository.OutboxEventRepository;
import com.jasper.invoice.infrastructure.persistence.InvoiceProcessingJobJpaRepository;
import com.jasper.invoice.infrastructure.persistence.OutboxEventJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@DirtiesContext
@TestPropertySource(properties = {
        "invoice.processing.mode=poller",
        "invoice.processing.scheduling-enabled=false",
})
class InvoiceJobCreationServiceRollbackIntegrationTest {

    @Autowired
    private InvoiceJobCreationService service;

    @Autowired
    private InvoiceProcessingJobJpaRepository jobRepository;

    @Autowired
    private OutboxEventJpaRepository outboxRepository;

    @MockitoBean
    private OutboxEventRepository outboxEventRepository;

    @BeforeEach
    void cleanUp() {
        outboxRepository.deleteAll();
        jobRepository.deleteAll();
    }

    @Test
    void shouldRollbackJobWhenOutboxSaveFails() {
        when(outboxEventRepository.save(any()))
                .thenThrow(new RuntimeException("outbox failure"));

        String documentReference = "document-" + UUID.randomUUID();

        assertThrows(
                RuntimeException.class,
                () -> service.createJob(documentReference)
        );

        assertTrue(
                jobRepository.findAll().stream()
                        .noneMatch(job ->
                                documentReference.equals(job.getDocumentReference()))
        );

        assertTrue(outboxRepository.findAll().isEmpty());
    }
}