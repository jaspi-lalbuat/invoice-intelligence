package com.jasper.invoice.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jasper.invoice.application.ProcessingClaim;
import com.jasper.invoice.domain.model.Invoice;
import com.jasper.invoice.domain.model.InvoiceProcessingJob;
import com.jasper.invoice.domain.validation.InvoiceValidationResult;
import org.springframework.stereotype.Component;

@Component
public class InvoiceProcessingJobMapper {

    private final ObjectMapper objectMapper;

    public InvoiceProcessingJobMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public InvoiceProcessingJobEntity toEntity(InvoiceProcessingJob job) {
        InvoiceProcessingJobEntity entity = new InvoiceProcessingJobEntity();

        mapState(entity, job);

        return entity;
    }

    public InvoiceProcessingJob toDomain(InvoiceProcessingJobEntity entity) {
        Invoice invoice = deserialize(
                entity.getInvoiceData(),
                Invoice.class
        );

        InvoiceValidationResult validationResult = deserialize(
                entity.getValidationData(),
                InvoiceValidationResult.class
        );

        return InvoiceProcessingJob.restore(
                        entity.getId(),
                entity.getStatus(),
                invoice,
                validationResult,
                entity.getDocumentReference(),
                entity.getRetryCount(),
                entity.getLeaseUntil(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
                );
    }

    public void updateClaimedEntity(
            InvoiceProcessingJobEntity entity,
            ProcessingClaim claim
    ) {
        mapState(entity, claim.job());

        entity.setProcessingAttemptId(
                claim.processingAttemptId()
        );
    }

    private void mapState(
            InvoiceProcessingJobEntity entity,
            InvoiceProcessingJob job
    ) {
        entity.setId(job.id());
        entity.setDocumentReference(job.documentReference());
        entity.setStatus(job.status());
        entity.setRetryCount(job.retryCount());
        entity.setLeaseUntil(job.leaseUntil());

        entity.setInvoiceData(
                serialize(job.invoice())
        );

        entity.setValidationData(
                serialize(job.validationResult())
        );
    }

    String serialize(Object value) {
        if (value == null) {
            return null;
        }

        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "Failed to serialize value",
                    e
            );
        }
    }

    private <T> T deserialize(
            String json,
            Class<T> type
    ) {
        if (json == null) {
            return null;
        }

        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "Failed to deserialize " + type.getSimpleName(),
                    e
            );
        }
    }
}