package com.jasper.invoice.application;

import java.util.UUID;

public record InvoiceProcessingRequested(
        UUID jobId
) {
}