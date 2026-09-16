package com.jasper.invoice.application;

import com.jasper.invoice.application.port.InvoiceProcessingEventPublisher;
import com.jasper.invoice.domain.outbox.OutboxEvent;
import com.jasper.invoice.domain.repository.OutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class OutboxPublisherTest {

    private final OutboxEventRepository outboxEventRepository =
            mock(OutboxEventRepository.class);

    private final InvoiceProcessingEventPublisher eventPublisher =
            mock(InvoiceProcessingEventPublisher.class);

    private final OutboxPublisher outboxPublisher =
            new OutboxPublisher(
                    outboxEventRepository,
                    eventPublisher
            );

    @Test
    void shouldPublishAndMarkEventAsPublished() {
        UUID jobId = UUID.randomUUID();

        OutboxEvent event = new OutboxEvent(
                UUID.randomUUID(),
                "InvoiceProcessingRequested",
                jobId,
                "{\"jobId\":\"" + jobId + "\"}",
                Instant.now()
        );

        when(outboxEventRepository.findUnpublished(100))
                .thenReturn(List.of(event));

        outboxPublisher.publishPendingEvents();

        InOrder inOrder = inOrder(
                eventPublisher,
                outboxEventRepository
        );

        inOrder.verify(eventPublisher)
                .publish(new InvoiceProcessingRequested(jobId));

        inOrder.verify(outboxEventRepository)
                .markPublished(event);
    }

    @Test
    void shouldNotMarkEventPublishedWhenPublishingFails() {
        UUID jobId = UUID.randomUUID();

        OutboxEvent event = new OutboxEvent(
                UUID.randomUUID(),
                "InvoiceProcessingRequested",
                jobId,
                "{\"jobId\":\"" + jobId + "\"}",
                Instant.now()
        );

        when(outboxEventRepository.findUnpublished(100))
                .thenReturn(List.of(event));

        doThrow(new RuntimeException("Kafka unavailable"))
                .when(eventPublisher)
                .publish(new InvoiceProcessingRequested(jobId));

        assertThatThrownBy(
                () -> outboxPublisher.publishPendingEvents()
        )
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Kafka unavailable");

        verify(eventPublisher)
                .publish(new InvoiceProcessingRequested(jobId));

        verify(outboxEventRepository, never())
                .markPublished(event);
    }

    @Test
    void shouldStopProcessingAfterPublishingFailure() {
        UUID firstJobId = UUID.randomUUID();
        UUID secondJobId = UUID.randomUUID();

        OutboxEvent firstEvent = new OutboxEvent(
                UUID.randomUUID(),
                "InvoiceProcessingRequested",
                firstJobId,
                "{}",
                Instant.now()
        );

        OutboxEvent secondEvent = new OutboxEvent(
                UUID.randomUUID(),
                "InvoiceProcessingRequested",
                secondJobId,
                "{}",
                Instant.now()
        );

        when(outboxEventRepository.findUnpublished(100))
                .thenReturn(List.of(firstEvent, secondEvent));

        doThrow(new RuntimeException("Kafka unavailable"))
                .when(eventPublisher)
                .publish(new InvoiceProcessingRequested(firstJobId));

        assertThatThrownBy(
                () -> outboxPublisher.publishPendingEvents()
        )
                .isInstanceOf(RuntimeException.class);

        verify(eventPublisher)
                .publish(new InvoiceProcessingRequested(firstJobId));

        verify(eventPublisher, never())
                .publish(new InvoiceProcessingRequested(secondJobId));

        verify(outboxEventRepository, never())
                .markPublished(firstEvent);

        verify(outboxEventRepository, never())
                .markPublished(secondEvent);
    }

    @Test
    void shouldRepublishEventWhenMarkPublishedFails() {
        UUID jobId = UUID.randomUUID();

        OutboxEvent event = new OutboxEvent(
                UUID.randomUUID(),
                "InvoiceProcessingRequested",
                jobId,
                "{\"jobId\":\"" + jobId + "\"}",
                Instant.now()
        );

        when(outboxEventRepository.findUnpublished(100))
                .thenReturn(List.of(event))
                .thenReturn(List.of(event));

        doThrow(new RuntimeException("database unavailable"))
                .when(outboxEventRepository)
                .markPublished(event);

        // First attempt: Kafka succeeds, marking published fails.
        assertThatThrownBy(
                () -> outboxPublisher.publishPendingEvents()
        )
                .isInstanceOf(RuntimeException.class)
                .hasMessage("database unavailable");

        verify(eventPublisher, times(1))
                .publish(new InvoiceProcessingRequested(jobId));

        // Second attempt: event is still unpublished and gets published again.
        doNothing()
                .when(outboxEventRepository)
                .markPublished(event);

        outboxPublisher.publishPendingEvents();

        verify(eventPublisher, times(2))
                .publish(new InvoiceProcessingRequested(jobId));

        verify(outboxEventRepository, times(2))
                .markPublished(event);
    }
}