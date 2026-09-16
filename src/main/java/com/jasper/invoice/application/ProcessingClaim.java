package com.jasper.invoice.application;

import com.jasper.invoice.domain.model.InvoiceProcessingJob;

import java.util.UUID;

public record ProcessingClaim(
    InvoiceProcessingJob job,
    UUID processingAttemptId
) {}