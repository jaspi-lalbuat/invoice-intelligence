package com.jasper.invoice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "invoice.processing")
public record ProcessingModeProperties(
        ProcessingMode mode
) {}