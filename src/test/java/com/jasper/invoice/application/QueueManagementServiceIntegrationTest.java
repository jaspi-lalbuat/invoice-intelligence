package com.jasper.invoice.application;

import com.jasper.invoice.application.port.ProcessingOwnershipStore;
import com.jasper.invoice.config.JacksonConfig;
import com.jasper.invoice.domain.model.ProcessingStatus;
import com.jasper.invoice.domain.model.InvoiceProcessingJob;
import com.jasper.invoice.domain.repository.InvoiceProcessingJobRepository;
import com.jasper.invoice.infrastructure.persistence.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(
    replace = AutoConfigureTestDatabase.Replace.NONE
)
@Import({
    QueueManagementService.class,
    JpaInvoiceProcessingJobRepository.class,
    InvoiceProcessingJobMapper.class,
        JpaProcessingOwnershipStore.class,
    JacksonConfig.class
})
class QueueManagementServiceIntegrationTest {

    @Autowired
    private QueueManagementService queueManagementService;

    @Autowired
    private InvoiceProcessingJobJpaRepository jpaRepository;

    @Autowired
    private InvoiceProcessingJobRepository repository;
    @Autowired
    private ProcessingOwnershipStore ownershipStore;

    @BeforeEach
    void cleanDatabase() {
        jpaRepository.deleteAll();
    }

    @Test
    void shouldClaimQueuedJob() {

        InvoiceProcessingJob job =
            new InvoiceProcessingJob(
                UUID.randomUUID(),
                "invoice.pdf"
            );

        repository.save(job);

        Optional<ProcessingClaim> claimed =
            queueManagementService.claimNextJob();

        assertTrue(claimed.isPresent());
        assertEquals(
            job.id(),
            claimed.get().job().id()
        );
        assertEquals(
            ProcessingStatus.PROCESSING,
            claimed.get().job().status()
        );

        InvoiceProcessingJobEntity persisted =
            jpaRepository.findById(job.id()).orElseThrow();

        assertEquals(
            ProcessingStatus.PROCESSING,
            persisted.getStatus()
        );
    }

    @Test
    void shouldNotClaimAlreadyProcessingJob() {

        InvoiceProcessingJob job =
                new InvoiceProcessingJob(
                        UUID.randomUUID(),
                        "invoice.pdf"
                );

        repository.save(job);

        Optional<ProcessingClaim> first =
                queueManagementService.claimNextJob();

        assertTrue(first.isPresent());

        Optional<ProcessingClaim> second =
                queueManagementService.claimNextJob();

        assertTrue(second.isEmpty());
    }

    @Test
    void shouldClaimQueuedJobAndSetLease() {

        InvoiceProcessingJob job =
                new InvoiceProcessingJob(
                        UUID.randomUUID(),
                        "invoice.pdf"
                );

        repository.save(job);

        Instant before = Instant.now();

        Optional<ProcessingClaim> claimed =
                queueManagementService.claimNextJob();

        Instant after = Instant.now();

        assertTrue(claimed.isPresent());

        InvoiceProcessingJob result =
                claimed.get().job();

        assertEquals(
                ProcessingStatus.PROCESSING,
                result.status()
        );

        assertNotNull(result.leaseUntil());

        assertFalse(result.leaseUntil().isBefore(
                before.plus(Duration.ofMinutes(5))
        ));

        assertFalse(result.leaseUntil().isAfter(
                after.plus(Duration.ofMinutes(5))
        ));

        InvoiceProcessingJobEntity persisted =
                jpaRepository.findById(job.id()).orElseThrow();

        assertEquals(
                ProcessingStatus.PROCESSING,
                persisted.getStatus()
        );

        assertNotNull(persisted.getLeaseUntil());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void shouldAllowOnlyOneWorkerToClaimAJob() throws Exception {

        InvoiceProcessingJob job =
                new InvoiceProcessingJob(
                        UUID.randomUUID(),
                        "invoice.pdf"
                );

        repository.save(job);

        ExecutorService executor =
                Executors.newFixedThreadPool(2);

        try {
            CountDownLatch start = new CountDownLatch(1);

            Callable<Optional<ProcessingClaim>> task = () -> {
                start.await();
                return queueManagementService.claimNextJob();
            };

            Future<Optional<ProcessingClaim>> first =
                    executor.submit(task);

            Future<Optional<ProcessingClaim>> second =
                    executor.submit(task);

            start.countDown();

            Optional<ProcessingClaim> result1 =
                    first.get();

            Optional<ProcessingClaim> result2 =
                    second.get();

            long successfulClaims =
                    Stream.of(result1, result2)
                            .filter(Optional::isPresent)
                            .count();

            assertEquals(1, successfulClaims);

            Optional<ProcessingClaim> claimed =
                    result1.or(() -> result2);

            assertEquals(job.id(), claimed.get().job().id());

        } finally {
            executor.shutdown();
        }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void shouldRecoverExpiredJob() {

        Instant expiredLease =
                Instant.now().minus(Duration.ofMinutes(1));

        InvoiceProcessingJob job =
                new InvoiceProcessingJob(
                        UUID.randomUUID(),
                        "invoice.pdf"
                );

        job.startProcessing(expiredLease);

        repository.save(job);

        int recovered =
                queueManagementService.recoverExpiredJobs();

        assertEquals(1, recovered);

        InvoiceProcessingJob recoveredJob =
                repository.findById(job.id()).orElseThrow();

        assertEquals(
                ProcessingStatus.QUEUED,
                recoveredJob.status()
        );

        assertNull(
                recoveredJob.leaseUntil()
        );

        ProcessingClaim secondClaim =
                queueManagementService.claimNextJob()
                        .orElseThrow();

        assertEquals(
                job.id(),
                secondClaim.job().id()
        );

        assertEquals(
                ProcessingStatus.PROCESSING,
                secondClaim.job().status()
        );

        assertNotNull(
                secondClaim.job().leaseUntil()
        );
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void shouldNotRecoverActiveJob() {

        Instant activeLease =
                Instant.now().plus(Duration.ofMinutes(2));

        InvoiceProcessingJob job =
                new InvoiceProcessingJob(
                        UUID.randomUUID(),
                        "invoice.pdf"
                );

        job.startProcessing(activeLease);

        repository.save(job);

        int recovered =
                queueManagementService.recoverExpiredJobs();

        assertEquals(0, recovered);

        InvoiceProcessingJob persisted =
                repository.findById(job.id()).orElseThrow();

        assertEquals(
                ProcessingStatus.PROCESSING,
                persisted.status()
        );

        assertEquals(
                activeLease,
                persisted.leaseUntil()
        );
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void shouldRecoverExpiredJobsAndIgnoreActiveJobs() {
        Instant now = Instant.now();

        InvoiceProcessingJob expiredJob1 =
                new InvoiceProcessingJob(
                        UUID.randomUUID(),
                        "expired-1.pdf"
                );

        expiredJob1.startProcessing(
                now.minus(Duration.ofMinutes(5))
        );

        InvoiceProcessingJob expiredJob2 =
                new InvoiceProcessingJob(
                        UUID.randomUUID(),
                        "expired-2.pdf"
                );

        expiredJob2.startProcessing(
                now.minus(Duration.ofMinutes(2))
        );

        InvoiceProcessingJob activeJob =
                new InvoiceProcessingJob(
                        UUID.randomUUID(),
                        "active.pdf"
                );

        Instant activeLease =
                now.plus(Duration.ofMinutes(2));

        activeJob.startProcessing(activeLease);

        repository.save(expiredJob1);
        repository.save(expiredJob2);
        repository.save(activeJob);

        int recovered =
                queueManagementService.recoverExpiredJobs();

        assertEquals(2, recovered);

        InvoiceProcessingJob recoveredJob1 =
                repository.findById(expiredJob1.id()).orElseThrow();

        InvoiceProcessingJob recoveredJob2 =
                repository.findById(expiredJob2.id()).orElseThrow();

        InvoiceProcessingJob persistedActiveJob =
                repository.findById(activeJob.id()).orElseThrow();

        assertEquals(
                ProcessingStatus.QUEUED,
                recoveredJob1.status()
        );

        assertNull(recoveredJob1.leaseUntil());

        assertEquals(
                ProcessingStatus.QUEUED,
                recoveredJob2.status()
        );

        assertNull(recoveredJob2.leaseUntil());

        assertEquals(
                ProcessingStatus.PROCESSING,
                persistedActiveJob.status()
        );

        assertEquals(
                activeLease,
                persistedActiveJob.leaseUntil()
        );
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void shouldReturnZeroWhenNoJobsAreExpired() {
        Instant now = Instant.now();

        InvoiceProcessingJob queuedJob =
                new InvoiceProcessingJob(
                        UUID.randomUUID(),
                        "queued.pdf"
                );

        InvoiceProcessingJob activeJob =
                new InvoiceProcessingJob(
                        UUID.randomUUID(),
                        "active.pdf"
                );

        activeJob.startProcessing(
                now.plus(Duration.ofMinutes(2))
        );

        repository.save(queuedJob);
        repository.save(activeJob);

        int recovered =
                queueManagementService.recoverExpiredJobs();

        assertEquals(0, recovered);

        assertEquals(
                ProcessingStatus.QUEUED,
                repository.findById(queuedJob.id()).orElseThrow().status()
        );

        assertEquals(
                ProcessingStatus.PROCESSING,
                repository.findById(activeJob.id()).orElseThrow().status()
        );
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void shouldRecoverJobWhoseLeaseExpiresNow() {
        Instant leaseUntil = Instant.now();

        InvoiceProcessingJob job =
                new InvoiceProcessingJob(
                        UUID.randomUUID(),
                        "invoice.pdf"
                );

        job.startProcessing(leaseUntil);

        repository.save(job);

        int recovered =
                queueManagementService.recoverExpiredJobs();

        assertEquals(1, recovered);

        InvoiceProcessingJob recoveredJob =
                repository.findById(job.id()).orElseThrow();

        assertEquals(
                ProcessingStatus.QUEUED,
                recoveredJob.status()
        );

        assertNull(recoveredJob.leaseUntil());
    }

    @Test
    void shouldRetryFailedJobAndMakeItClaimableAgain() {
        UUID jobId = UUID.randomUUID();

        InvoiceProcessingJob job =
                new InvoiceProcessingJob(
                        jobId,
                        "invoice.pdf"
                );

        repository.save(job);

        // First attempt
        ProcessingClaim firstClaim =
                queueManagementService.claimNextJob()
                        .orElseThrow();

        assertEquals(
                ProcessingStatus.PROCESSING,
                firstClaim.job().status()
        );

        // Simulate terminal processing failure
        firstClaim.job().fail();
        repository.save(firstClaim.job());

        // Manual retry
        queueManagementService.retryJob(jobId);

        InvoiceProcessingJob retried =
                repository.findById(jobId).orElseThrow();

        assertEquals(
                ProcessingStatus.QUEUED,
                retried.status()
        );

        assertEquals(
                1,
                retried.retryCount()
        );

        // Second attempt should be claimable
        ProcessingClaim secondClaim =
                queueManagementService.claimNextJob()
                        .orElseThrow();

        assertEquals(
                ProcessingStatus.PROCESSING,
                secondClaim.job().status()
        );

        assertNotEquals(
                firstClaim.processingAttemptId(),
                secondClaim.processingAttemptId()
        );
    }

}
