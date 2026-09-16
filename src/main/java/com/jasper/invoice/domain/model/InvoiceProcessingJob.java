package com.jasper.invoice.domain.model;

import com.jasper.invoice.domain.validation.InvoiceValidationResult;
import com.jasper.invoice.domain.validation.ValidationStatus;

import java.time.Instant;
import java.util.UUID;

public class InvoiceProcessingJob {

    private final UUID id;
    private ProcessingStatus status;
    private Invoice invoice;
    private InvoiceValidationResult validationResult;
    private final String documentReference;
    private int retryCount;
    private Instant leaseUntil;
    private final Instant createdAt;
    private Instant updatedAt;

    public InvoiceProcessingJob(UUID id, String documentReference) {
        this.id = id;
        this.documentReference = documentReference;
        this.status = ProcessingStatus.QUEUED;
        this.retryCount = 0;
        this.leaseUntil = null;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    private InvoiceProcessingJob(
            UUID id,
            ProcessingStatus status,
            Invoice invoice,
            InvoiceValidationResult validationResult,
            String documentReference,
            int retryCount,
            Instant leaseUntil,
            Instant createdAt,
            Instant updatedAt) {

        this.id = id;
        this.status = status;
        this.invoice = invoice;
        this.validationResult = validationResult;
        this.leaseUntil = leaseUntil;
        this.documentReference = documentReference;
        this.retryCount = retryCount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static InvoiceProcessingJob restore(
            UUID id,
            ProcessingStatus status,
            Invoice invoice,
            InvoiceValidationResult validationResult,
            String documentReference,
            int retryCount,
            Instant leaseUntil,
            Instant createdAt,
            Instant updatedAt) {

        return new InvoiceProcessingJob(
                id,
                status,
                invoice,
                validationResult,
                documentReference,
                retryCount,
                leaseUntil,
                createdAt,
                updatedAt
        );
    }

    public UUID id() {
        return id;
    }

    public ProcessingStatus status() {
        return status;
    }

    public Invoice invoice() {
        return invoice;
    }

    public InvoiceValidationResult validationResult() {
        return validationResult;
    }
    public String documentReference() {
        return documentReference;
    }
    public int retryCount() {
        return retryCount;
    }
    public Instant leaseUntil() {
        return leaseUntil;
    }

    public void startProcessing(Instant leaseUntil) {
        if (status != ProcessingStatus.QUEUED) {
            throw new InvalidProcessingStateException(
                    "Job cannot start processing from status: " + status
            );
        }

        this.status = ProcessingStatus.PROCESSING;
        this.leaseUntil = leaseUntil;
    }

    public void complete(
            Invoice invoice,
            InvoiceValidationResult validationResult) {
        if (status != ProcessingStatus.PROCESSING) {
            throw new InvalidProcessingStateException(
                    "Cannot complete processing from status: " + status
            );
        }

        this.invoice = invoice;
        this.validationResult = validationResult;

        this.status = validationResult.status() == ValidationStatus.VALID
                ? ProcessingStatus.READY
                : ProcessingStatus.REVIEW_REQUIRED;
    }

    public void fail() {

        if (status != ProcessingStatus.PROCESSING) {
            throw new InvalidProcessingStateException(
                    "Cannot fail processing from status: " + status
            );
        }

        this.status = ProcessingStatus.FAILED;
    }

    public void retry() {

        if (status != ProcessingStatus.FAILED) {
            throw new InvalidProcessingStateException(
                    "Job can only be retried from FAILED"
            );
        }

        retryCount++;
        status = ProcessingStatus.QUEUED;
    }

    public ProcessingStatus retryOrFail(int maxAutomaticRetries) {
        if (status != ProcessingStatus.PROCESSING) {
            throw new InvalidProcessingStateException(
                    "Cannot retry or fail job in state: " + status
            );
        }

        if (retryCount < maxAutomaticRetries) {
            retryCount++;
            status = ProcessingStatus.QUEUED;
            leaseUntil = null;
            updatedAt = Instant.now();
            return ProcessingStatus.QUEUED;
        }

        status = ProcessingStatus.FAILED;
        leaseUntil = null;
        updatedAt = Instant.now();
        return ProcessingStatus.FAILED;
    }

    public void requeueAfterLeaseExpiry(Instant now) {
        if (status != ProcessingStatus.PROCESSING) {
            throw new InvalidProcessingStateException(
                    "Job cannot be requeued from status: " + status
            );
        }

        if (leaseUntil == null || leaseUntil.isAfter(now)) {
            throw new InvalidProcessingStateException(
                    "Job lease has not expired"
            );
        }

        this.status = ProcessingStatus.QUEUED;
        this.leaseUntil = null;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
