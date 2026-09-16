package com.jasper.invoice.infrastructure.document;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tesseract")
public record TesseractProperties(
        String datapath,
        String language
) {}