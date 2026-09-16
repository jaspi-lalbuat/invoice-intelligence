package com.jasper.invoice.application;

import java.util.UUID;

public class InvoiceProcessingJobNotFoundException
        extends RuntimeException {

    public InvoiceProcessingJobNotFoundException(UUID jobId) {
        super("Processing job not found: " + jobId);
    }
}