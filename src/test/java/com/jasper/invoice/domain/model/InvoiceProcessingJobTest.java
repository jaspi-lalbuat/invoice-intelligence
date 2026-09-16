package com.jasper.invoice.domain.model;

import com.jasper.invoice.application.document.InvalidProcessingStateException;
import com.jasper.invoice.application.document.ProcessingStatus;
import com.jasper.invoice.domain.validation.InvoiceValidationResult;
import com.jasper.invoice.domain.validation.ValidationStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class InvoiceProcessingJobTest {
    String documentReference = "placeholder-" + UUID.randomUUID();
    Instant leaseUntil = Instant.now().plusSeconds(0);

    @Test
    void shouldStartInQueuedState() {

        InvoiceProcessingJob job =
                new InvoiceProcessingJob(UUID.randomUUID(), documentReference);

        Assertions.assertEquals(
                ProcessingStatus.QUEUED,
                job.status()
        );
    }

    @Test
    void shouldTransitionFromQueuedToProcessing() {

        InvoiceProcessingJob job =
                new InvoiceProcessingJob(UUID.randomUUID(), documentReference);

        job.startProcessing(leaseUntil);

        assertEquals(
                ProcessingStatus.PROCESSING,
                job.status()
        );
        assertEquals(
                leaseUntil,
                job.leaseUntil()
        );
    }

    @Test
    void shouldTransitionToReadyWhenValidationPasses() {

        InvoiceProcessingJob job =
                new InvoiceProcessingJob(UUID.randomUUID(), documentReference);

        job.startProcessing(leaseUntil);

        job.complete(
                null,
                validationResult(ValidationStatus.VALID)
        );

        assertEquals(
                ProcessingStatus.READY,
                job.status()
        );
        assertEquals(
                leaseUntil,
                job.leaseUntil()
        );
    }

    @Test
    void shouldTransitionToReviewRequiredWhenValidationFails() {

        InvoiceProcessingJob job =
                new InvoiceProcessingJob(UUID.randomUUID(), documentReference);

        job.startProcessing(leaseUntil);

        job.complete(
                null,
                validationResult(ValidationStatus.REVIEW_REQUIRED)
        );

        assertEquals(
                ProcessingStatus.REVIEW_REQUIRED,
                job.status()
        );
        assertEquals(
                leaseUntil,
                job.leaseUntil()
        );
    }

    @Test
    void shouldTransitionToFailed() {

        InvoiceProcessingJob job =
                new InvoiceProcessingJob(UUID.randomUUID(), documentReference);

        job.startProcessing(leaseUntil);
        job.fail();

        assertEquals(
                ProcessingStatus.FAILED,
                job.status()
        );
        assertEquals(
                leaseUntil,
                job.leaseUntil()
        );
    }

    @Test
    void shouldNotCompleteQueuedJob() {

        InvoiceProcessingJob job =
                new InvoiceProcessingJob(UUID.randomUUID(), documentReference);

        assertThrows(
                InvalidProcessingStateException.class,
                () -> job.complete(
                        null,
                        validationResult(ValidationStatus.VALID)
                )
        );
    }

    @Test
    void shouldNotStartAlreadyProcessingJob() {

        InvoiceProcessingJob job =
                new InvoiceProcessingJob(UUID.randomUUID(), documentReference);

        job.startProcessing(leaseUntil);

        assertThrows(
                InvalidProcessingStateException.class,
                () -> job.startProcessing(leaseUntil)
        );
    }

    @Test
    void shouldNotFailQueuedJob() {

        InvoiceProcessingJob job =
                new InvoiceProcessingJob(UUID.randomUUID(), documentReference);

        assertThrows(
                InvalidProcessingStateException.class,
                job::fail
        );
    }

    private InvoiceValidationResult validationResult(
            ValidationStatus status) {

        return new InvoiceValidationResult(
                status,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                List.of()
        );
    }

    @Test
    void shouldRequeueAfterLeaseExpires() {

        Instant now = Instant.parse("2026-09-10T10:00:00Z");
        Instant leaseUntil = now.plusSeconds(120);

        InvoiceProcessingJob job =
                new InvoiceProcessingJob(
                        UUID.randomUUID(),
                        "invoice.pdf"
                );

        job.startProcessing(leaseUntil);

        job.requeueAfterLeaseExpiry(
                now.plusSeconds(121)
        );

        assertEquals(
                ProcessingStatus.QUEUED,
                job.status()
        );

        assertNull(job.leaseUntil());
    }

    @Test
    void shouldNotRequeueBeforeLeaseExpires() {

        Instant now = Instant.parse("2026-09-10T10:00:00Z");
        Instant leaseUntil = now.plusSeconds(120);

        InvoiceProcessingJob job =
                new InvoiceProcessingJob(
                        UUID.randomUUID(),
                        "invoice.pdf"
                );

        job.startProcessing(leaseUntil);

        assertThrows(
                InvalidProcessingStateException.class,
                () -> job.requeueAfterLeaseExpiry(
                        now.plusSeconds(60)
                )
        );

        assertEquals(
                ProcessingStatus.PROCESSING,
                job.status()
        );
    }

    @Test
    void shouldNotRequeueQueuedJob() {
        Instant now = Instant.parse("2026-09-10T10:00:00Z");

        InvoiceProcessingJob job =
                new InvoiceProcessingJob(
                        UUID.randomUUID(),
                        "invoice.pdf"
                );

        assertThrows(
                InvalidProcessingStateException.class,
                () -> job.requeueAfterLeaseExpiry(now)
        );

        assertEquals(
                ProcessingStatus.QUEUED,
                job.status()
        );
    }

    @Test
    void shouldSetLeaseWhenProcessingStarts() {
        Instant leaseUntil =
                Instant.parse("2026-09-10T10:02:00Z");

        InvoiceProcessingJob job =
                new InvoiceProcessingJob(
                        UUID.randomUUID(),
                        "invoice.pdf"
                );

        job.startProcessing(leaseUntil);

        assertEquals(
                ProcessingStatus.PROCESSING,
                job.status()
        );

        assertEquals(
                leaseUntil,
                job.leaseUntil()
        );
    }
}