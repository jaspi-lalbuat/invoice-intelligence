package com.jasper.invoice.application.document;

public enum ProcessingStatus {
    QUEUED,
    PROCESSING,
    READY,
    REVIEW_REQUIRED,
    FAILED
}