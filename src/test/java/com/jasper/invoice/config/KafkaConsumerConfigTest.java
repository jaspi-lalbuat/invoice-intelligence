package com.jasper.invoice.config;

import com.jasper.invoice.application.ProcessingRetryPolicy;
import com.jasper.invoice.application.InvoiceProcessingRequested;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KafkaConsumerConfigTest {

    @Test
    void shouldDisableAutoCommitForManualAcknowledgments() {
        ProcessingRetryPolicy retryPolicy = new ProcessingRetryPolicy(
                3,
                Duration.ofMinutes(5),
                Duration.ofMinutes(6)
        );

        DefaultKafkaConsumerFactory<String, InvoiceProcessingRequested> factory =
                (DefaultKafkaConsumerFactory<String, InvoiceProcessingRequested>)
                        new KafkaConsumerConfig().consumerFactory(retryPolicy);

        assertEquals(
                false,
                factory.getConfigurationProperties()
                        .get(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG)
        );
        assertEquals(
                360_000,
                factory.getConfigurationProperties()
                        .get(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG)
        );
    }
}
