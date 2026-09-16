package com.jasper.invoice.application;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class OutboxPublishingPollerTest {

    private final OutboxPublisher outboxPublisher =
            mock(OutboxPublisher.class);

    private final OutboxPublishingPoller poller =
            new OutboxPublishingPoller(outboxPublisher);

    @Test
    void shouldPublishPendingEvents() {
        poller.poll();

        verify(outboxPublisher)
                .publishPendingEvents();
    }
}