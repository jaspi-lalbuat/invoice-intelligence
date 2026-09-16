package com.jasper.invoice.application;

public enum ProcessingResult {
    COMPLETED,
    RETRIED,
    FAILED,
    STALE
}