package com.jasper.invoice.application;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        prefix = "invoice.processing",
        name = "scheduling-enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class InvoiceProcessingRecoveryPoller {

    private final QueueManagementService queueManagementService;

    public InvoiceProcessingRecoveryPoller(
            QueueManagementService queueManagementService
    ) {
        this.queueManagementService = queueManagementService;
    }

    @Scheduled(fixedDelayString = "${invoice.processing.recovery-interval}")
    public void recover() {
        queueManagementService.recoverExpiredJobs();
    }
}