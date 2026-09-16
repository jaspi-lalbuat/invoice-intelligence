package com.jasper.invoice.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jasper.invoice.domain.model.InvoiceProcessingJob;
import com.jasper.invoice.domain.outbox.OutboxEvent;
import com.jasper.invoice.domain.repository.InvoiceProcessingJobRepository;
import com.jasper.invoice.domain.repository.OutboxEventRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@AllArgsConstructor
public class InvoiceJobCreationService {

    private final InvoiceProcessingJobRepository repository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public InvoiceProcessingJob createJob(String documentReference, String originalFileName) {

        InvoiceProcessingJob job =
                new InvoiceProcessingJob(
                        UUID.randomUUID(),
                        documentReference,
                        originalFileName
                );

        InvoiceProcessingJob savedJob =
                repository.save(job);

        InvoiceProcessingRequested event =
                new InvoiceProcessingRequested(savedJob.id());

        String payload;

        try {
            payload = objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "Failed to serialize invoice processing event",
                    e
            );
        }

        outboxEventRepository.save(
                new OutboxEvent(
                        UUID.randomUUID(),
                        "InvoiceProcessingRequested",
                        savedJob.id(),
                        payload,
                        Instant.now()
                )
        );

        return savedJob;
    }
}