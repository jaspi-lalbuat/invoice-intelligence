package com.jasper.invoice.application;

import com.jasper.invoice.application.port.ProcessingOwnershipStore;
import com.jasper.invoice.domain.model.InvoiceProcessingJob;
import com.jasper.invoice.domain.repository.InvoiceProcessingJobRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class QueueManagementService {

    private static final Duration LEASE_DURATION =
            Duration.ofMinutes(2);

    private final InvoiceProcessingJobRepository repository;
    private final ProcessingOwnershipStore ownershipStore;

    public QueueManagementService(
            InvoiceProcessingJobRepository repository,
            ProcessingOwnershipStore ownershipStore) {
        this.repository = repository;
        this.ownershipStore = ownershipStore;
    }

    @Transactional
    public Optional<ProcessingClaim> claimNextJob() {

        Optional<InvoiceProcessingJob> result =
                repository.findNextQueuedJobForUpdate();

        if (result.isEmpty()) {
            return Optional.empty();
        }

        InvoiceProcessingJob job = result.get();

        Instant leaseUntil =
                Instant.now().plus(LEASE_DURATION);

        // Domain transition
        job.startProcessing(leaseUntil);

        UUID processingAttemptId = UUID.randomUUID();

        ProcessingClaim claim =
                new ProcessingClaim(job, processingAttemptId);

        ownershipStore.persistClaim(claim);

        return Optional.of(claim);
    }

    @Transactional
    public Optional<ProcessingClaim> claimJob(UUID jobId) {

        Optional<InvoiceProcessingJob> result =
                repository.findByIdForUpdate(jobId);

        if (result.isEmpty()) {
            return Optional.empty();
        }

        InvoiceProcessingJob job = result.get();

        Instant leaseUntil =
                Instant.now().plus(LEASE_DURATION);

        // Domain transition
        job.startProcessing(leaseUntil);

        UUID processingAttemptId = UUID.randomUUID();

        ProcessingClaim claim =
                new ProcessingClaim(job, processingAttemptId);

        ownershipStore.persistClaim(claim);

        return Optional.of(claim);
    }

    @Transactional
    public int recoverExpiredJobs() {

        Instant now = Instant.now();

        List<InvoiceProcessingJob> jobs =
                repository.findExpiredProcessingJobs(now);

        for (InvoiceProcessingJob job : jobs) {
            job.requeueAfterLeaseExpiry(now);
            repository.save(job);
        }

        return jobs.size();
    }

    @Transactional
    public void retryJob(UUID jobId) {
        InvoiceProcessingJob job = repository.findById(jobId)
                .orElseThrow(() ->
                        new InvoiceProcessingJobNotFoundException(jobId)
                );

        job.retry();
        repository.save(job);
    }
}