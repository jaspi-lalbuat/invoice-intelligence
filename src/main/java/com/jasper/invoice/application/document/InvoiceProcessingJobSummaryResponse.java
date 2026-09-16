package com.jasper.invoice.application.document;

import com.jasper.invoice.domain.model.ProcessingStatus;

import java.time.Instant;
import java.util.UUID;

public record InvoiceProcessingJobSummaryResponse(
        UUID jobId,
        String originalFileName,
        ProcessingStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}