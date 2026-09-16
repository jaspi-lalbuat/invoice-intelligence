package com.jasper.invoice.infrastructure.persistence;

import com.jasper.invoice.config.JacksonConfig;
import com.jasper.invoice.domain.model.InvoiceProcessingJob;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DataJpaTest
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)
@Import({InvoiceProcessingJobMapper.class,
        JacksonConfig.class})
class InvoiceProcessingJobPersistenceIntegrationTest {

    @Autowired
    private InvoiceProcessingJobJpaRepository jpaRepository;

    @Autowired
    private InvoiceProcessingJobMapper mapper;

    @Test
    void shouldPersistAndRestoreProcessingJob() {

        InvoiceProcessingJob job =
                new InvoiceProcessingJob(
                        UUID.randomUUID(),
                        "invoice.pdf"
                );

        InvoiceProcessingJobEntity entity =
                mapper.toEntity(job);

        jpaRepository.saveAndFlush(entity);

        InvoiceProcessingJobEntity persisted =
                jpaRepository.findById(job.id())
                        .orElseThrow();

        InvoiceProcessingJob restored =
                mapper.toDomain(persisted);

        assertEquals(job.id(), restored.id());
        assertEquals(job.documentReference(), restored.documentReference());
        assertEquals(job.status(), restored.status());

        assertNotNull(persisted.getCreatedAt());
        assertNotNull(persisted.getUpdatedAt());

        assertNotNull(restored.createdAt());
        assertNotNull(restored.updatedAt());
    }
}