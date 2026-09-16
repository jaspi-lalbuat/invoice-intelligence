package com.jasper.invoice.application;

import com.jasper.invoice.application.document.DocumentService;
import com.jasper.invoice.application.document.InvoiceExtractionResponse;
import com.jasper.invoice.domain.model.ProcessingStatus;
import com.jasper.invoice.application.document.port.DocumentStorage;
import com.jasper.invoice.application.port.InvoiceProcessingEventPublisher;
import com.jasper.invoice.application.port.ProcessingOwnershipStore;
import com.jasper.invoice.domain.model.InvoiceProcessingJob;
import com.jasper.invoice.domain.validation.InvoiceValidationResult;
import com.jasper.invoice.domain.validation.ValidationStatus;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class InvoiceProcessingWorkerTest {

    private final DocumentStorage documentStorage =
            mock(DocumentStorage.class);

    private final DocumentService documentService =
            mock(DocumentService.class);

    private final ProcessingOwnershipStore ownershipStore =
            mock(ProcessingOwnershipStore.class);


    private final InvoiceProcessingEventPublisher eventPublisher =
            mock(InvoiceProcessingEventPublisher.class);

    private final InvoiceProcessingWorker worker =
            new InvoiceProcessingWorker(
                    documentStorage,
                    documentService,
                    ownershipStore,
                    eventPublisher
            );

    @Test
    void shouldCompleteJobWhenProcessingSucceeds() throws Exception {
        UUID jobId = UUID.randomUUID();
        UUID attemptId = UUID.randomUUID();

        InvoiceProcessingJob job =
                new InvoiceProcessingJob(jobId, "invoice.pdf");

        job.startProcessing(
                Instant.now().plusSeconds(120)
        );

        ProcessingClaim claim =
                new ProcessingClaim(job, attemptId);

        InvoiceValidationResult validation =
                new InvoiceValidationResult(
                        ValidationStatus.VALID,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        List.of()
                );

        InvoiceExtractionResponse response =
                new InvoiceExtractionResponse(null, validation);

        when(documentStorage.load("invoice.pdf"))
                .thenReturn(
                        new ByteArrayInputStream(new byte[0])
                );

        when(documentService.extractInvoice(any()))
                .thenReturn(response);

        when(ownershipStore.complete(claim))
                .thenReturn(true);

        ProcessingResult result =
                worker.process(claim);

        assertEquals(
                ProcessingResult.COMPLETED,
                result
        );

        assertEquals(
                ProcessingStatus.READY,
                job.status()
        );

        verify(ownershipStore).complete(claim);
    }

    @Test
    void shouldDiscardResultWhenWorkerLosesOwnership() throws Exception {
        UUID jobId = UUID.randomUUID();
        UUID attemptId = UUID.randomUUID();

        InvoiceProcessingJob job =
                new InvoiceProcessingJob(jobId, "invoice.pdf");

        job.startProcessing(
                java.time.Instant.now().plusSeconds(120)
        );

        ProcessingClaim claim =
                new ProcessingClaim(job, attemptId);

        InvoiceValidationResult validation =
                new InvoiceValidationResult(
                        ValidationStatus.VALID,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        List.of()
                );

        InvoiceExtractionResponse result =
                new InvoiceExtractionResponse(null, validation);

        when(documentStorage.load("invoice.pdf"))
                .thenReturn(
                        new ByteArrayInputStream(new byte[0])
                );

        when(documentService.extractInvoice(any()))
                .thenReturn(result);

        when(ownershipStore.complete(claim))
                .thenReturn(false);

        worker.process(claim);

        assertEquals(
                ProcessingStatus.READY,
                job.status()
        );

        verify(ownershipStore).complete(claim);
    }

    @Test
    void shouldFailJobWhenProcessingThrows() throws Exception {
        UUID jobId = UUID.randomUUID();
        UUID attemptId = UUID.randomUUID();

        InvoiceProcessingJob job =
                new InvoiceProcessingJob(jobId, "invoice.pdf");

        job.startProcessing(
                Instant.now().plusSeconds(120)
        );

        ProcessingClaim claim =
                new ProcessingClaim(job, attemptId);

        when(documentStorage.load("invoice.pdf"))
                .thenReturn(
                        new ByteArrayInputStream(new byte[0])
                );

        when(documentService.extractInvoice(any()))
                .thenThrow(new RuntimeException("LLM failed"));

        when(ownershipStore.retryOrFail(claim))
                .thenReturn(ProcessingFailureResult.FAILED);

        assertDoesNotThrow(
                () -> worker.process(claim)
        );

        verify(ownershipStore).retryOrFail(claim);
        verify(ownershipStore, never()).complete(any());
    }

    @Test
    void shouldDiscardFailureWhenWorkerLosesOwnership() throws Exception {
        UUID jobId = UUID.randomUUID();
        UUID attemptId = UUID.randomUUID();

        InvoiceProcessingJob job =
                new InvoiceProcessingJob(jobId, "invoice.pdf");

        job.startProcessing(
                Instant.now().plusSeconds(120)
        );

        ProcessingClaim claim =
                new ProcessingClaim(job, attemptId);

        when(documentStorage.load("invoice.pdf"))
                .thenThrow(new RuntimeException("LLM failed"));

        when(ownershipStore.retryOrFail(claim))
                .thenReturn(ProcessingFailureResult.STALE);

        assertDoesNotThrow(
                () -> worker.process(claim)
        );

        verify(ownershipStore).retryOrFail(claim);
        verify(ownershipStore, never()).complete(any());
    }

    @Test
    void shouldRequeueWhenProcessingFailsAndRetryIsAvailable() throws Exception {
        UUID jobId = UUID.randomUUID();
        UUID attemptId = UUID.randomUUID();

        InvoiceProcessingJob job =
                new InvoiceProcessingJob(jobId, "invoice.pdf");

        job.startProcessing(
                Instant.now().plusSeconds(120)
        );

        ProcessingClaim claim =
                new ProcessingClaim(job, attemptId);

        when(documentStorage.load("invoice.pdf"))
                .thenThrow(new RuntimeException("LLM failed"));

        when(ownershipStore.retryOrFail(claim))
                .thenReturn(ProcessingFailureResult.RETRIED);

        assertDoesNotThrow(
                () -> worker.process(claim)
        );

        verify(ownershipStore).retryOrFail(claim);
        verify(ownershipStore, never()).complete(any());
    }
}
