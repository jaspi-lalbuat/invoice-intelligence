package com.jasper.invoice.application;

import com.jasper.invoice.domain.model.ProcessingStatus;
import com.jasper.invoice.application.port.ProcessingOwnershipStore;
import com.jasper.invoice.config.JacksonConfig;
import com.jasper.invoice.domain.model.InvoiceProcessingJob;
import com.jasper.invoice.domain.validation.InvoiceValidationResult;
import com.jasper.invoice.domain.validation.ValidationStatus;
import com.jasper.invoice.infrastructure.persistence.InvoiceProcessingJobEntity;
import com.jasper.invoice.infrastructure.persistence.InvoiceProcessingJobJpaRepository;
import com.jasper.invoice.infrastructure.persistence.InvoiceProcessingJobMapper;
import com.jasper.invoice.infrastructure.persistence.JpaProcessingOwnershipStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)
@Import({
        JpaProcessingOwnershipStore.class,
        InvoiceProcessingJobMapper.class,
        JacksonConfig.class
})
class JpaProcessingOwnershipStoreIntegrationTest {

    @Autowired
    private ProcessingOwnershipStore ownershipStore;

    @Autowired
    private InvoiceProcessingJobJpaRepository jpaRepository;

    @Autowired
    private InvoiceProcessingJobMapper mapper;

    @BeforeEach
    void cleanDatabase() {
        jpaRepository.deleteAll();
    }

    @Test
    void shouldCompleteJobWhenAttemptIdMatches() {
        UUID jobId = UUID.randomUUID();
        UUID attemptId = UUID.randomUUID();

        InvoiceProcessingJob job =
                new InvoiceProcessingJob(jobId, "invoice.pdf", "document-ref");

        jpaRepository.save(mapper.toEntity(job));

        job.startProcessing(Instant.now().plusSeconds(120));

        ProcessingClaim claim =
                new ProcessingClaim(job, attemptId);

        ownershipStore.persistClaim(claim);

        InvoiceValidationResult validation =
                new InvoiceValidationResult(
                        ValidationStatus.VALID,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        List.of()
                );

        job.complete(null, validation);

        boolean completed = ownershipStore.complete(claim);

        assertTrue(completed);

        InvoiceProcessingJobEntity entity =
                jpaRepository.findById(jobId).orElseThrow();

        assertEquals(ProcessingStatus.READY, entity.getStatus());
        assertNull(entity.getProcessingAttemptId());
        assertNull(entity.getLeaseUntil());
    }

    @Test
    void shouldRejectCompletionFromStaleWorker() {
        UUID jobId = UUID.randomUUID();

        UUID currentAttemptId = UUID.randomUUID();
        UUID staleAttemptId = UUID.randomUUID();

        InvoiceProcessingJob job =
                new InvoiceProcessingJob(jobId, "invoice.pdf", "document-ref");

        jpaRepository.save(mapper.toEntity(job));

        job.startProcessing(Instant.now().plusSeconds(120));

        ProcessingClaim currentClaim =
                new ProcessingClaim(job, currentAttemptId);

        ownershipStore.persistClaim(currentClaim);

        ProcessingClaim staleClaim =
                new ProcessingClaim(job, staleAttemptId);

        InvoiceValidationResult validation =
                new InvoiceValidationResult(
                        ValidationStatus.VALID,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        List.of()
                );

        job.complete(null, validation);

        boolean completed =
                ownershipStore.complete(staleClaim);

        assertFalse(completed);

        InvoiceProcessingJobEntity entity =
                jpaRepository.findById(jobId).orElseThrow();

        assertEquals(
                ProcessingStatus.PROCESSING,
                entity.getStatus()
        );

        assertEquals(
                currentAttemptId,
                entity.getProcessingAttemptId()
        );
    }

    @Test
    void shouldFailJobWhenAttemptIdMatches() {
        UUID jobId = UUID.randomUUID();
        UUID attemptId = UUID.randomUUID();

        InvoiceProcessingJob job =
                new InvoiceProcessingJob(jobId, "invoice.pdf", "document-ref");

        jpaRepository.save(mapper.toEntity(job));

        // First attempt
        job.startProcessing(Instant.now().plusSeconds(120));

        ProcessingClaim claim =
                new ProcessingClaim(job, attemptId);

        ownershipStore.persistClaim(claim);

        // Simulate three previous automatic retries.
        job.retryOrFail(3);
        job.startProcessing(Instant.now().plusSeconds(120));
        job.retryOrFail(3);
        job.startProcessing(Instant.now().plusSeconds(120));
        job.retryOrFail(3);
        job.startProcessing(Instant.now().plusSeconds(120));

        // Fourth failure: retry limit exhausted.
        ProcessingFailureResult result =
                ownershipStore.retryOrFail(claim);

        assertEquals(
                ProcessingFailureResult.FAILED,
                result
        );

        InvoiceProcessingJobEntity entity =
                jpaRepository.findById(jobId).orElseThrow();

        assertEquals(
                ProcessingStatus.FAILED,
                entity.getStatus()
        );

        assertEquals(
                3,
                entity.getRetryCount()
        );

        assertNull(entity.getProcessingAttemptId());
        assertNull(entity.getLeaseUntil());
    }

    @Test
    void shouldRejectFailureFromStaleWorker() {
        UUID jobId = UUID.randomUUID();

        UUID currentAttemptId = UUID.randomUUID();
        UUID staleAttemptId = UUID.randomUUID();

        InvoiceProcessingJob job =
                new InvoiceProcessingJob(jobId, "invoice.pdf", "document-ref");

        jpaRepository.save(mapper.toEntity(job));

        job.startProcessing(Instant.now().plusSeconds(120));

        ProcessingClaim currentClaim =
                new ProcessingClaim(job, currentAttemptId);

        ownershipStore.persistClaim(currentClaim);

        ProcessingClaim staleClaim =
                new ProcessingClaim(job, staleAttemptId);

        ProcessingFailureResult result =
                ownershipStore.retryOrFail(staleClaim);

        assertEquals(
                ProcessingFailureResult.STALE,
                result
        );

        InvoiceProcessingJobEntity entity =
                jpaRepository.findById(jobId).orElseThrow();

        assertEquals(
                ProcessingStatus.PROCESSING,
                entity.getStatus()
        );

        assertEquals(
                currentAttemptId,
                entity.getProcessingAttemptId()
        );
    }
}
