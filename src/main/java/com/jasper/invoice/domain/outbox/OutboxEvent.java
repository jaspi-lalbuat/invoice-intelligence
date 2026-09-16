package com.jasper.invoice.domain.outbox;

import java.time.Instant;
import java.util.UUID;

public record OutboxEvent(
        UUID id,
        String eventType,
        UUID aggregateId,
        String payload,
        Instant createdAt
) {
}