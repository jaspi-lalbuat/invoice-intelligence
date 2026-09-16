package com.jasper.invoice.application;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        prefix = "invoice.processing",
        name = "mode",
        havingValue = "poller"
)
public class InvoiceProcessingPoller {
    private final QueueManagementService queueManagementService;
    private final InvoiceProcessingWorker worker;

    public InvoiceProcessingPoller(
            QueueManagementService queueManagementService,
            InvoiceProcessingWorker worker) {
        this.queueManagementService = queueManagementService;
        this.worker = worker;
    }

    @Scheduled(fixedDelayString = "${invoice.processing.poll-interval}")
    public void poll() {
        queueManagementService
            .claimNextJob()
            .ifPresent(worker::process);
    }
}