package com.jasper.invoice.infrastructure.messaging;

import com.jasper.invoice.application.InvoiceProcessingRequested;
import com.jasper.invoice.application.InvoiceProcessingWorker;
import com.jasper.invoice.application.ProcessingResult;
import com.jasper.invoice.application.QueueManagementService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
@ConditionalOnProperty(
        prefix = "invoice.processing",
        name = "mode",
        havingValue = "kafka"
)
public class KafkaInvoiceProcessingConsumer {

    private final QueueManagementService queueManagementService;
    private final InvoiceProcessingWorker worker;

    public KafkaInvoiceProcessingConsumer(
            QueueManagementService queueManagementService,
            InvoiceProcessingWorker worker
    ) {
        this.queueManagementService = queueManagementService;
        this.worker = worker;
    }

    @KafkaListener(
            topics = "invoice-processing",
            groupId = "invoice-processing-worker"
    )
    public void consume(
            InvoiceProcessingRequested event,
            Acknowledgment acknowledgment
    ) {
        ProcessingResult result =
                queueManagementService
                        .claimJob(event.jobId())
                        .map(worker::process)
                        .orElse(ProcessingResult.STALE);

        if (result == ProcessingResult.RETRIED) {
            acknowledgment.nack(Duration.ofSeconds(1));
            return;
        }

        acknowledgment.acknowledge();
    }
}