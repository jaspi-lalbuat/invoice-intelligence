package com.jasper.invoice.infrastructure.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jasper.invoice.domain.model.InvoiceProcessingJob;
import com.jasper.invoice.domain.model.ProcessingStatus;
import com.jasper.invoice.domain.model.Invoice;
import com.jasper.invoice.domain.model.InvoiceLineItem;
import com.jasper.invoice.domain.model.Party;
import com.jasper.invoice.domain.model.TaxBreakdown;
import com.jasper.invoice.infrastructure.persistence.InvoiceProcessingJobEntity;
import com.jasper.invoice.infrastructure.persistence.InvoiceProcessingJobMapper;
import com.jasper.invoice.domain.validation.InvoiceValidationResult;
import com.jasper.invoice.domain.validation.ValidationStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InvoiceProcessingJobMapperTest {

    private final ObjectMapper objectMapper =
            new ObjectMapper().findAndRegisterModules();

    private final InvoiceProcessingJobMapper mapper =
            new InvoiceProcessingJobMapper(objectMapper);

    @Test
    void shouldRoundTripAggregate() {

        UUID jobId = UUID.randomUUID();
        String documentReference = "placeholder-" + UUID.randomUUID();
        Instant leaseUntil = Instant.now().plusSeconds(0);

        Invoice invoice = new Invoice(
                "INV-001",
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30),
                "INR",
                new Party("Supplier", null, "GST123"),
                new Party("Customer", null, "GST456"),
                List.of(
                        new InvoiceLineItem(
                                "Laptop",
                                new BigDecimal("2"),
                                new BigDecimal("50000"),
                                new BigDecimal("18"),
                                null
                        )
                ),
                new BigDecimal("100000"),
                new TaxBreakdown(
                        new BigDecimal("9000"),
                        new BigDecimal("9000"),
                        BigDecimal.ZERO
                ),
                BigDecimal.ZERO,
                new BigDecimal("118000")
        );

        InvoiceValidationResult validation =
                new InvoiceValidationResult(
                        ValidationStatus.VALID,
                        new BigDecimal("100000"),
                        BigDecimal.ZERO,
                        new BigDecimal("118000"),
                        BigDecimal.ZERO,
                        List.of()
                );

        InvoiceProcessingJob original =
                InvoiceProcessingJob.restore(
                        jobId,
                        ProcessingStatus.READY,
                        invoice,
                        validation,
                        documentReference,
                        0,
                        leaseUntil,
                        Instant.now(),
                        Instant.now()
                );

        InvoiceProcessingJobEntity entity =
                mapper.toEntity(original);

        InvoiceProcessingJob restored =
                mapper.toDomain(entity);

        assertEquals(original.id(), restored.id());
        assertEquals(original.status(), restored.status());
        assertEquals(original.invoice(), restored.invoice());
        assertEquals(
                original.validationResult(),
                restored.validationResult()
        );
        assertEquals(original.retryCount(), restored.retryCount());
    }
}
