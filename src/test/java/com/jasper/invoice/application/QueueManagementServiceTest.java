package com.jasper.invoice.application;

import com.jasper.invoice.application.port.ProcessingOwnershipStore;
import com.jasper.invoice.domain.model.InvalidProcessingStateException;
import com.jasper.invoice.domain.model.ProcessingStatus;
import com.jasper.invoice.domain.model.InvoiceProcessingJob;
import com.jasper.invoice.domain.repository.InvoiceProcessingJobRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QueueManagementServiceTest {

    @Mock
    private InvoiceProcessingJobRepository repository;
    @Mock
    private ProcessingOwnershipStore ownershipStore;
    @Mock
    private ProcessingRetryPolicy retryPolicy;

    @InjectMocks
    private QueueManagementService service;

    @BeforeEach
    void configureTiming() {
        lenient().when(retryPolicy.leaseDuration())
                .thenReturn(Duration.ofMinutes(5));
    }

    @Test
    void shouldClaimQueuedJob() {
        InvoiceProcessingJob job =
                new InvoiceProcessingJob(
                        UUID.randomUUID(),
                        "invoice.pdf"
                );

        when(repository.findNextQueuedJobForUpdate())
                .thenReturn(Optional.of(job));

        Optional<ProcessingClaim> claim =
                service.claimNextJob();

        assertTrue(claim.isPresent());

        assertEquals(
                ProcessingStatus.PROCESSING,
                claim.get().job().status()
        );

        assertNotNull(claim.get().processingAttemptId());

        verify(repository)
                .findNextQueuedJobForUpdate();

        verify(ownershipStore)
                .persistClaim(claim.get());

        verify(repository, never())
                .save(any(InvoiceProcessingJob.class));
    }

    @Test
    void shouldReturnEmptyWhenNoQueuedJobExists() {
        when(repository.findNextQueuedJobForUpdate())
            .thenReturn(Optional.empty());

        Optional<ProcessingClaim> result =
            service.claimNextJob();

        assertTrue(result.isEmpty());

        verify(repository).findNextQueuedJobForUpdate();
        verify(repository, never()).save(any());
    }

    @Test
    void shouldRetryFailedJob() {
        UUID jobId = UUID.randomUUID();

        InvoiceProcessingJob job =
                new InvoiceProcessingJob(jobId, "invoice.pdf");

        job.startProcessing(Instant.now().plusSeconds(120));
        job.fail();

        when(repository.findById(jobId))
                .thenReturn(Optional.of(job));

        service.retryJob(jobId);

        assertEquals(
                ProcessingStatus.QUEUED,
                job.status()
        );

        assertEquals(1, job.retryCount());

        verify(repository).findById(jobId);
        verify(repository).save(job);
    }

    @Test
    void shouldRejectRetryWhenJobIsNotFailed() {
        UUID jobId = UUID.randomUUID();

        InvoiceProcessingJob job =
                new InvoiceProcessingJob(jobId, "invoice.pdf");

        when(repository.findById(jobId))
                .thenReturn(Optional.of(job));

        assertThrows(
                InvalidProcessingStateException.class,
                () -> service.retryJob(jobId)
        );

        verify(repository, never()).save(any());
    }
}
