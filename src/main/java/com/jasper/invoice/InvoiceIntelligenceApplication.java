package com.jasper.invoice;

import com.jasper.invoice.application.ProcessingRetryPolicy;
import com.jasper.invoice.config.ProcessingModeProperties;
import com.jasper.invoice.infrastructure.document.TesseractProperties;
import com.jasper.invoice.infrastructure.extraction.OllamaProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({
        OllamaProperties.class,
        ProcessingRetryPolicy.class,
        ProcessingModeProperties.class,
        TesseractProperties.class
})
public class InvoiceIntelligenceApplication {

    public static void main(String[] args) {
        SpringApplication.run(InvoiceIntelligenceApplication.class, args);
    }

}
