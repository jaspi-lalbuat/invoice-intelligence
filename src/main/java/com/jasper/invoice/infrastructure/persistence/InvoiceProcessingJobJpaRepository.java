package com.jasper.invoice.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvoiceProcessingJobJpaRepository
        extends JpaRepository<InvoiceProcessingJobEntity, UUID> {

    @Query(value = """
            SELECT *
    FROM invoice_processing_jobs
    WHERE id = :jobId
      AND status = 'QUEUED'
    FOR UPDATE SKIP LOCKED
    """, nativeQuery = true)
    Optional<InvoiceProcessingJobEntity> findByIdForUpdate(@Param("jobId") UUID id);

    @Query(value = """
    SELECT *
    FROM invoice_processing_jobs
    WHERE status = 'QUEUED'
    ORDER BY id
    FOR UPDATE SKIP LOCKED
    LIMIT 1
    """, nativeQuery = true)
    Optional<InvoiceProcessingJobEntity> findNextQueuedJobForUpdate();

    @Query(value = """
    SELECT *
    FROM invoice_processing_jobs
    WHERE status = 'PROCESSING'
      AND lease_until <= :now
    FOR UPDATE SKIP LOCKED
    LIMIT 100
    """, nativeQuery = true)
    List<InvoiceProcessingJobEntity> findExpiredProcessingJobs(Instant now);

    @Modifying(
            flushAutomatically = true,
            clearAutomatically = true
    )
    @Query(value = """
    UPDATE invoice_processing_jobs
    SET
        status = :status,
        invoice_data = CAST(:invoiceData AS jsonb),
        validation_data = CAST(:validationData AS jsonb),
        retry_count = :retryCount,
        lease_until = :leaseUntil,
        processing_attempt_id = :processingAttemptId
    WHERE id = :id
    """, nativeQuery = true)
    int persistClaim(
            @Param("id") UUID id,
            @Param("status") String status,
            @Param("invoiceData") String invoiceData,
            @Param("validationData") String validationData,
            @Param("retryCount") int retryCount,
            @Param("leaseUntil") Instant leaseUntil,
            @Param("processingAttemptId") UUID processingAttemptId
    );

    @Modifying
    @Query(value = """
    UPDATE invoice_processing_jobs
    SET
        status = :status,
        invoice_data = CAST(:invoiceData AS jsonb),
        validation_data = CAST(:validationData AS jsonb),
        lease_until = NULL,
        processing_attempt_id = NULL
    WHERE id = :id
      AND status = 'PROCESSING'
      AND processing_attempt_id = :processingAttemptId
    """, nativeQuery = true)
    int complete(
            @Param("id") UUID id,
            @Param("status") String status,
            @Param("invoiceData") String invoiceData,
            @Param("validationData") String validationData,
            @Param("processingAttemptId") UUID processingAttemptId
    );

    @Modifying(
            flushAutomatically = true,
            clearAutomatically = true
    )
    @Query(value = """
        UPDATE invoice_processing_jobs
        SET
            status = :status,
            retry_count = :retryCount,
            lease_until = NULL,
            processing_attempt_id = NULL
        WHERE id = :id
          AND status = 'PROCESSING'
          AND processing_attempt_id = :processingAttemptId
        """, nativeQuery = true)
    int retryOrFail(
            UUID id,
            String status,
            int retryCount,
            UUID processingAttemptId
    );

    List<InvoiceProcessingJobEntity> findAllByOrderByCreatedAtDesc();
}