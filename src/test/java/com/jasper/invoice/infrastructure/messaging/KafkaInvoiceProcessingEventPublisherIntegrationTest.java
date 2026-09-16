package com.jasper.invoice.infrastructure.messaging;

import com.jasper.invoice.application.InvoiceProcessingRequested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@SpringBootTest
class KafkaInvoiceProcessingEventPublisherIntegrationTest {

    @Autowired
    private KafkaInvoiceProcessingEventPublisher publisher;

    @Test
    void shouldPublishProcessingRequestedEvent() {
        InvoiceProcessingRequested event =
                new InvoiceProcessingRequested(UUID.randomUUID());

        assertDoesNotThrow(() -> publisher.publish(event));
    }
}