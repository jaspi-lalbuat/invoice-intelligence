package com.jasper.invoice.application.document;

import com.jasper.invoice.domain.model.Invoice;
import com.jasper.invoice.domain.validation.InvoiceValidationResult;

import java.time.Instant;
import java.util.UUID;

public record InvoiceProcessingJobResponse(
        UUID jobId,
        ProcessingStatus status,
        Invoice invoice,
        InvoiceValidationResult validation,
        Instant createdAt,
        Instant updatedAt
) {}