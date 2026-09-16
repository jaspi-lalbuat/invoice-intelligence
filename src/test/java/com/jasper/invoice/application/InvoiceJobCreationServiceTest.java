package com.jasper.invoice.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jasper.invoice.domain.model.InvoiceProcessingJob;
import com.jasper.invoice.domain.outbox.OutboxEvent;
import com.jasper.invoice.domain.repository.InvoiceProcessingJobRepository;
import com.jasper.invoice.domain.repository.OutboxEventRepository;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class InvoiceJobCreationServiceTest {

    private final InvoiceProcessingJobRepository repository =
            mock(InvoiceProcessingJobRepository.class);

    private final OutboxEventRepository outboxEventRepository =
            mock(OutboxEventRepository.class);

    private final ObjectMapper objectMapper =
            mock(ObjectMapper.class);

    private final InvoiceJobCreationService service =
            new InvoiceJobCreationService(
                    repository,
                    outboxEventRepository,
                    objectMapper
            );

    @Test
    void shouldCreateJobAndOutboxEvent() throws Exception {
        String documentReference =
                "placeholder-" + UUID.randomUUID();

        when(repository.save(any(InvoiceProcessingJob.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(objectMapper.writeValueAsString(
                any(InvoiceProcessingRequested.class)))
                .thenReturn("{\"jobId\":\"test\"}");

        InvoiceProcessingJob job =
                service.createJob(documentReference);

        assertNotNull(job);
        assertNotNull(job.id());
        assertEquals(documentReference, job.documentReference());

        verify(repository).save(job);
        verify(outboxEventRepository).save(any(OutboxEvent.class));
    }

    @Test
    void shouldSaveOutboxEventWithCreatedJobId() throws Exception {
        String documentReference =
                "placeholder-" + UUID.randomUUID();

        when(repository.save(any(InvoiceProcessingJob.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(objectMapper.writeValueAsString(
                any(InvoiceProcessingRequested.class)))
                .thenReturn("{\"jobId\":\"test\"}");

        InvoiceProcessingJob job =
                service.createJob(documentReference);

        verify(outboxEventRepository).save(
                argThat(event ->
                        event.aggregateId().equals(job.id())
                                && event.eventType()
                                .equals("InvoiceProcessingRequested")
                )
        );
    }

    @Test
    void shouldNotSaveOutboxEventWhenEventSerializationFails()
            throws Exception {

        String documentReference =
                "placeholder-" + UUID.randomUUID();

        when(repository.save(any(InvoiceProcessingJob.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(objectMapper.writeValueAsString(
                any(InvoiceProcessingRequested.class)))
                .thenThrow(new JsonProcessingException("serialization failed") {});

        assertThrows(
                IllegalStateException.class,
                () -> service.createJob(documentReference)
        );

        verify(repository).save(any(InvoiceProcessingJob.class));
        verify(outboxEventRepository, never())
                .save(any(OutboxEvent.class));
    }
}