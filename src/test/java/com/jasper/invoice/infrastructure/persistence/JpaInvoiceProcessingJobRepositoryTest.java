package com.jasper.invoice.infrastructure.persistence;

import com.jasper.invoice.config.JacksonConfig;
import com.jasper.invoice.application.document.ProcessingStatus;
import com.jasper.invoice.domain.model.*;
import com.jasper.invoice.domain.repository.InvoiceProcessingJobRepository;
import com.jasper.invoice.infrastructure.persistence.InvoiceProcessingJobJpaRepository;
import com.jasper.invoice.infrastructure.persistence.InvoiceProcessingJobMapper;
import com.jasper.invoice.infrastructure.persistence.JpaInvoiceProcessingJobRepository;
import com.jasper.invoice.domain.validation.InvoiceValidationResult;
import com.jasper.invoice.domain.validation.ValidationStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;




import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
        JpaInvoiceProcessingJobRepository.class,
        InvoiceProcessingJobMapper.class,
        JacksonConfig.class
})
class JpaInvoiceProcessingJobRepositoryTest {

    @Autowired
    private InvoiceProcessingJobRepository repository;

    @Autowired
    private InvoiceProcessingJobJpaRepository jpaRepository;

    @Test
    void shouldSaveAndFindJob() {

        UUID id = UUID.randomUUID();
        String documentReference = "placeholder-" + UUID.randomUUID();

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

        Instant leaseUntil = Instant.now().plusSeconds(0);
        InvoiceProcessingJob job =
                InvoiceProcessingJob.restore(
                        id,
                        ProcessingStatus.READY,
                        invoice,
                        validation,
                        documentReference,
                        0,
                        leaseUntil,
                        Instant.now(),
                        Instant.now()
                );

        repository.save(job);

        Optional<InvoiceProcessingJob> result =
                repository.findById(id);

        assertTrue(result.isPresent());

        InvoiceProcessingJob restored = result.get();

        assertEquals(id, restored.id());
        assertEquals(ProcessingStatus.READY, restored.status());
        assertEquals(invoice, restored.invoice());
        assertEquals(validation, restored.validationResult());
    }

    @Test
    void shouldSaveAndFindQueuedJob() {

        UUID id = UUID.randomUUID();
        String documentReference = "placeholder-" + UUID.randomUUID();

        InvoiceProcessingJob job =
                new InvoiceProcessingJob(id, documentReference);

        repository.save(job);

        Optional<InvoiceProcessingJob> result =
                repository.findById(id);

        assertTrue(result.isPresent());

        InvoiceProcessingJob restored = result.get();

        assertEquals(id, restored.id());
        assertEquals(ProcessingStatus.QUEUED, restored.status());
        assertNull(restored.invoice());
        assertNull(restored.validationResult());
    }
}