package com.jasper.invoice.infrastructure.messaging;

import com.jasper.invoice.application.InvoiceProcessingRequested;
import com.jasper.invoice.application.port.InvoiceProcessingEventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class KafkaInvoiceProcessingEventPublisher
        implements InvoiceProcessingEventPublisher {

    private static final String TOPIC = "invoice-processing";

    private final KafkaTemplate<String, InvoiceProcessingRequested> kafkaTemplate;

    public KafkaInvoiceProcessingEventPublisher(
            KafkaTemplate<String, InvoiceProcessingRequested> kafkaTemplate
    ) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void publish(InvoiceProcessingRequested event) {
        try {
            kafkaTemplate.send(
                    TOPIC,
                    event.jobId().toString(),
                    event
            ).get();

            log.info(
                    "Invoice processing event published to Kafka: jobId={}, topic={}",
                    event.jobId(),
                    TOPIC
            );

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "Interrupted while publishing invoice processing event",
                    e
            );

        } catch (Exception e) {
            log.error(
                    "Failed to publish invoice processing event: jobId={}, topic={}",
                    event.jobId(),
                    TOPIC,
                    e
            );

            throw new IllegalStateException(
                    "Failed to publish invoice processing event: " + event.jobId(),
                    e
            );
        }
    }
}