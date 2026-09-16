package com.jasper.invoice.application;

import com.jasper.invoice.application.document.DocumentService;
import com.jasper.invoice.application.document.InvoiceExtractionResponse;
import com.jasper.invoice.application.document.port.DocumentStorage;
import com.jasper.invoice.application.port.InvoiceProcessingEventPublisher;
import com.jasper.invoice.application.port.ProcessingOwnershipStore;
import com.jasper.invoice.domain.model.InvoiceProcessingJob;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Slf4j
@Service
@AllArgsConstructor
public class InvoiceProcessingWorker {

    private final DocumentStorage documentStorage;
    private final DocumentService documentService;
    private final ProcessingOwnershipStore ownershipStore;
    private final InvoiceProcessingEventPublisher eventPublisher;

    public ProcessingResult process(ProcessingClaim claim) {
        long startNanos = System.nanoTime();
        InvoiceProcessingJob job = claim.job();

        log.info(
                "Invoice processing started: jobId={}, attemptId={}",
                job.id(),
                claim.processingAttemptId()
        );

        InvoiceExtractionResponse response;

        try (InputStream inputStream =
                     documentStorage.load(job.documentReference())) {

            response = documentService.extractInvoice(inputStream);

        } catch (Exception e) {
            long durationMillis =
                    (System.nanoTime() - startNanos) / 1_000_000;

            ProcessingFailureResult result =
                    ownershipStore.retryOrFail(claim);

            switch (result) {
                case RETRIED -> log.warn(
                        "Invoice processing failed; job requeued: jobId={}, attemptId={}, retryCount={}, durationMs={}",
                        job.id(),
                        claim.processingAttemptId(),
                        job.retryCount(),
                        durationMillis,
                        e
                );

                case FAILED -> log.error(
                        "Invoice processing failed permanently: jobId={}, attemptId={}, retryCount={}, durationMs={}",
                        job.id(),
                        claim.processingAttemptId(),
                        job.retryCount(),
                        durationMillis,
                        e
                );

                case STALE -> log.warn(
                        "Invoice processing failure discarded: jobId={}, attemptId={}, durationMs={}",
                        job.id(),
                        claim.processingAttemptId(),
                        durationMillis
                );
            }

            return switch (result) {
                case RETRIED -> ProcessingResult.RETRIED;
                case FAILED -> ProcessingResult.FAILED;
                case STALE -> ProcessingResult.STALE;
            };
        }

        job.complete(
                response.invoice(),
                response.validation()
        );

        boolean completed = ownershipStore.complete(claim);

        long durationMillis =
                (System.nanoTime() - startNanos) / 1_000_000;

        if (!completed) {
            log.warn(
                    "Invoice processing result discarded: jobId={}, attemptId={}, durationMs={}",
                    job.id(),
                    claim.processingAttemptId(),
                    durationMillis
            );

            return ProcessingResult.STALE;
        }

        log.info(
                "Invoice processing completed: jobId={}, attemptId={}, status={}, durationMs={}",
                job.id(),
                claim.processingAttemptId(),
                job.status(),
                durationMillis
        );

        return ProcessingResult.COMPLETED;
    }
}