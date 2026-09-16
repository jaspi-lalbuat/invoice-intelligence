package com.jasper.invoice.domain.repository;

import com.jasper.invoice.domain.model.InvoiceProcessingJob;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvoiceProcessingJobRepository {

    InvoiceProcessingJob save(InvoiceProcessingJob job);

    Optional<InvoiceProcessingJob> findById(UUID id);
    Optional<InvoiceProcessingJob> findNextQueuedJobForUpdate();
    Optional<InvoiceProcessingJob> findByIdForUpdate(UUID id);
    List<InvoiceProcessingJob> findExpiredProcessingJobs(Instant now);
}