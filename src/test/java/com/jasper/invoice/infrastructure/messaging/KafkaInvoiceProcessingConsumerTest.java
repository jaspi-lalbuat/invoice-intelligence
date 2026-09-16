package com.jasper.invoice.infrastructure.messaging;

import com.jasper.invoice.application.*;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.Acknowledgment;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;

class KafkaInvoiceProcessingConsumerTest {

    private final QueueManagementService queueManagementService =
            mock(QueueManagementService.class);

    private final InvoiceProcessingWorker worker =
            mock(InvoiceProcessingWorker.class);

    private final Acknowledgment acknowledgment =
            mock(Acknowledgment.class);

    private final KafkaInvoiceProcessingConsumer consumer =
            new KafkaInvoiceProcessingConsumer(
                    queueManagementService,
                    worker
            );

    @Test
    void shouldAcknowledgeCompletedProcessing() {
        UUID jobId = UUID.randomUUID();

        ProcessingClaim claim =
                mock(ProcessingClaim.class);

        when(queueManagementService.claimJob(jobId))
                .thenReturn(Optional.of(claim));

        when(worker.process(claim))
                .thenReturn(ProcessingResult.COMPLETED);

        consumer.consume(
                new InvoiceProcessingRequested(jobId),
                acknowledgment
        );

        verify(acknowledgment).acknowledge();
        verify(acknowledgment, never()).nack(any(Duration.class));
    }

    @Test
    void shouldAcknowledgePermanentFailure() {
        UUID jobId = UUID.randomUUID();

        ProcessingClaim claim =
                mock(ProcessingClaim.class);

        when(queueManagementService.claimJob(jobId))
                .thenReturn(Optional.of(claim));

        when(worker.process(claim))
                .thenReturn(ProcessingResult.FAILED);

        consumer.consume(
                new InvoiceProcessingRequested(jobId),
                acknowledgment
        );

        verify(acknowledgment).acknowledge();
        verify(acknowledgment, never()).nack(any(Duration.class));
    }

    @Test
    void shouldNackWhenProcessingIsRetried() {
        UUID jobId = UUID.randomUUID();

        ProcessingClaim claim =
                mock(ProcessingClaim.class);

        when(queueManagementService.claimJob(jobId))
                .thenReturn(Optional.of(claim));

        when(worker.process(claim))
                .thenReturn(ProcessingResult.RETRIED);

        consumer.consume(
                new InvoiceProcessingRequested(jobId),
                acknowledgment
        );

        verify(acknowledgment).nack(Duration.ofSeconds(1));
        verify(acknowledgment, never()).acknowledge();
    }

    @Test
    void shouldAcknowledgeWhenJobCannotBeClaimed() {
        UUID jobId = UUID.randomUUID();

        when(queueManagementService.claimJob(jobId))
                .thenReturn(Optional.empty());

        consumer.consume(
                new InvoiceProcessingRequested(jobId),
                acknowledgment
        );

        verify(acknowledgment).acknowledge();
        verify(acknowledgment, never()).nack(any(Duration.class));
        verifyNoInteractions(worker);
    }
}