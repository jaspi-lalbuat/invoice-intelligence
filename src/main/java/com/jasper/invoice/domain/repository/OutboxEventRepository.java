package com.jasper.invoice.domain.repository;

import com.jasper.invoice.domain.outbox.OutboxEvent;

import java.util.List;

public interface OutboxEventRepository {

    OutboxEvent save(OutboxEvent event);
    List<OutboxEvent> findUnpublished(int limit);
    void markPublished(OutboxEvent event);
}