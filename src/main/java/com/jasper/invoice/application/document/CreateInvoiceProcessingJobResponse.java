package com.jasper.invoice.application.document;

import java.util.UUID;

public record CreateInvoiceProcessingJobResponse(
        UUID jobId
) {}