package com.jasper.invoice.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "invoice.processing")
public record ProcessingRetryPolicy(
        int maxAutomaticRetries
) {}