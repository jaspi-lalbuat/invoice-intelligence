package com.jasper.invoice.application;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        prefix = "invoice.processing",
        name = "mode",
        havingValue = "kafka"
)
public class OutboxPublishingPoller {

    private final OutboxPublisher outboxPublisher;

    public OutboxPublishingPoller(OutboxPublisher outboxPublisher) {
        this.outboxPublisher = outboxPublisher;
    }

    @Scheduled(fixedDelayString = "${invoice.processing.outbox-poll-interval}")
    public void poll() {
        outboxPublisher.publishPendingEvents();
    }
}