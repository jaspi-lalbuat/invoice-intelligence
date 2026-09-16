package com.jasper.invoice.application;

import com.jasper.invoice.domain.outbox.OutboxEvent;
import com.jasper.invoice.domain.repository.OutboxEventRepository;
import com.jasper.invoice.application.port.InvoiceProcessingEventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class OutboxPublisher {

    private static final int BATCH_SIZE = 100;

    private final OutboxEventRepository outboxEventRepository;
    private final InvoiceProcessingEventPublisher eventPublisher;

    public OutboxPublisher(
            OutboxEventRepository outboxEventRepository,
            InvoiceProcessingEventPublisher eventPublisher
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.eventPublisher = eventPublisher;
    }

    public void publishPendingEvents() {
        List<OutboxEvent> events =
                outboxEventRepository.findUnpublished(BATCH_SIZE);

        for (OutboxEvent event : events) {

            log.info(
                    "Publishing outbox event: outboxEventId={}, eventType={}, aggregateId={}",
                    event.id(),
                    event.eventType(),
                    event.aggregateId()
            );

            eventPublisher.publish(
                    new InvoiceProcessingRequested(event.aggregateId())
            );

            outboxEventRepository.markPublished(event);

            log.info(
                    "Outbox event marked published: outboxEventId={}, aggregateId={}",
                    event.id(),
                    event.aggregateId()
            );
        }
    }
}