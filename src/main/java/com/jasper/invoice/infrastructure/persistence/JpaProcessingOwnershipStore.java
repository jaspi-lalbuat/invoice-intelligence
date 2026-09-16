package com.jasper.invoice.infrastructure.persistence;

import com.jasper.invoice.application.ProcessingClaim;
import com.jasper.invoice.application.ProcessingRetryPolicy;
import com.jasper.invoice.application.document.ProcessingStatus;
import com.jasper.invoice.application.ProcessingFailureResult;
import com.jasper.invoice.application.port.ProcessingOwnershipStore;
import com.jasper.invoice.domain.model.InvoiceProcessingJob;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaProcessingOwnershipStore
        implements ProcessingOwnershipStore {

    private final InvoiceProcessingJobJpaRepository repository;
    private final InvoiceProcessingJobMapper mapper;
    private final ProcessingRetryPolicy retryPolicy;

    public JpaProcessingOwnershipStore(
            InvoiceProcessingJobJpaRepository repository,
            InvoiceProcessingJobMapper mapper,
            ProcessingRetryPolicy retryPolicy
    ) {
        this.repository = repository;
        this.mapper = mapper;
        this.retryPolicy = retryPolicy;
    }

    @Override
    public void persistClaim(ProcessingClaim claim) {
        InvoiceProcessingJob job = claim.job();

        int updated = repository.persistClaim(
                job.id(),
                job.status().name(),
                mapper.serialize(job.invoice()),
                mapper.serialize(job.validationResult()),
                job.retryCount(),
                job.leaseUntil(),
                claim.processingAttemptId()
        );

        if (updated != 1) {
            throw new IllegalStateException(
                    "Failed to persist processing claim: " + job.id()
            );
        }
    }

    @Transactional
    @Override
    public boolean complete(ProcessingClaim claim) {
        InvoiceProcessingJob job = claim.job();

        int updated = repository.complete(
                job.id(),
                job.status().name(),
                mapper.serialize(job.invoice()),
                mapper.serialize(job.validationResult()),
                claim.processingAttemptId()
        );

        return updated == 1;
    }

    @Transactional
    @Override
    public ProcessingFailureResult retryOrFail(ProcessingClaim claim) {

        InvoiceProcessingJob job = claim.job();

        ProcessingStatus resultingStatus =
                job.retryOrFail(retryPolicy.maxAutomaticRetries());

        int updated = repository.retryOrFail(
                job.id(),
                resultingStatus.name(),
                job.retryCount(),
                claim.processingAttemptId()
        );

        if (updated != 1) {
            return ProcessingFailureResult.STALE;
        }

        return resultingStatus == ProcessingStatus.QUEUED
                ? ProcessingFailureResult.RETRIED
                : ProcessingFailureResult.FAILED;
    }

}