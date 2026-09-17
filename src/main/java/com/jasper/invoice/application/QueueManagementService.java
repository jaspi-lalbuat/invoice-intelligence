package com.jasper.invoice.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jasper.invoice.application.port.ProcessingOwnershipStore;
import com.jasper.invoice.domain.model.InvoiceProcessingJob;
import com.jasper.invoice.domain.outbox.OutboxEvent;
import com.jasper.invoice.domain.repository.InvoiceProcessingJobRepository;
import com.jasper.invoice.domain.repository.OutboxEventRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@AllArgsConstructor
@Service
public class QueueManagementService {

    private final InvoiceProcessingJobRepository repository;
    private final ProcessingOwnershipStore ownershipStore;
    private final ProcessingRetryPolicy retryPolicy;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public Optional<ProcessingClaim> claimNextJob() {

        Optional<InvoiceProcessingJob> result =
                repository.findNextQueuedJobForUpdate();

        if (result.isEmpty()) {
            return Optional.empty();
        }

        InvoiceProcessingJob job = result.get();

        Instant leaseUntil =
                Instant.now().plus(retryPolicy.leaseDuration());

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
                Instant.now().plus(retryPolicy.leaseDuration());

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
        InvoiceProcessingJob job =
                repository.findById(jobId)
                        .orElseThrow(() ->
                                new InvoiceProcessingJobNotFoundException(jobId)
                        );

        job.retry();

        InvoiceProcessingJob savedJob =
                repository.save(job);

        InvoiceProcessingRequested event =
                new InvoiceProcessingRequested(savedJob.id());

        String payload;

        try {
            payload = objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "Failed to serialize invoice processing event",
                    e
            );
        }

        outboxEventRepository.save(
                new OutboxEvent(
                        UUID.randomUUID(),
                        "InvoiceProcessingRequested",
                        savedJob.id(),
                        payload,
                        Instant.now()
                )
        );
    }
}
