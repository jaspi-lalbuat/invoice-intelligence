package com.jasper.invoice.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "invoice.processing")
public record ProcessingRetryPolicy(
        int maxAutomaticRetries,
        Duration leaseDuration,
        Duration maxKafkaPollInterval
) {}
