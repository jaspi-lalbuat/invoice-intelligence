package com.jasper.invoice.application;

import com.jasper.invoice.application.document.DocumentService;
import com.jasper.invoice.application.document.InvoiceExtractionResponse;
import com.jasper.invoice.domain.model.ProcessingStatus;
import com.jasper.invoice.domain.model.Invoice;
import com.jasper.invoice.domain.model.InvoiceProcessingJob;
import com.jasper.invoice.domain.model.Party;
import com.jasper.invoice.domain.model.TaxBreakdown;
import com.jasper.invoice.domain.repository.InvoiceProcessingJobRepository;
import com.jasper.invoice.domain.validation.InvoiceValidationResult;
import com.jasper.invoice.domain.validation.ValidationStatus;
import com.jasper.invoice.infrastructure.persistence.InvoiceProcessingJobJpaRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(
        properties = "spring.task.scheduling.enabled=false"
)
@TestPropertySource(properties = {
        "invoice.processing.mode=poller"
})
class InvoiceProcessingFlowIntegrationTest {

    @Autowired
    private InvoiceProcessingPoller poller;

    @Autowired
    private InvoiceProcessingService processingService;

    @Autowired
    private InvoiceProcessingJobRepository repository;

    @MockitoBean
    private DocumentService documentService;

    @Autowired
    private InvoiceProcessingJobJpaRepository jpaRepository;

    @BeforeEach
    void cleanDatabase() {
        jpaRepository.deleteAll();
    }

    @Test
    void shouldProcessSubmittedInvoice() throws Exception {

        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "invoice.pdf",
                        "application/pdf",
                        createValidPdf()
                );

        InvoiceProcessingJob created =
                processingService.submitInvoice(file);

        assertEquals(
                ProcessingStatus.QUEUED,
                created.status()
        );

        Invoice invoice = new Invoice(
                "INV-001",
                LocalDate.now(),
                null,
                "INR",
                new Party("Supplier", null, null),
                new Party("Customer", null, null),
                List.of(),
                BigDecimal.ZERO,
                new TaxBreakdown(
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO
                ),
                BigDecimal.ZERO,
                BigDecimal.ZERO
        );

        InvoiceValidationResult validation =
                new InvoiceValidationResult(
                        ValidationStatus.VALID,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        List.of()
                );

        when(documentService.extractInvoice(any(InputStream.class)))
                .thenReturn(
                        new InvoiceExtractionResponse(
                                invoice,
                                validation
                        )
                );

        poller.poll();

        InvoiceProcessingJob processed =
                repository.findById(created.id())
                        .orElseThrow();

        assertEquals(
                ProcessingStatus.READY,
                processed.status()
        );

        assertNotNull(processed.invoice());
        assertNotNull(processed.validationResult());
    }

    private byte[] createValidPdf() throws IOException {

        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {

            document.addPage(new PDPage());
            document.save(output);

            return output.toByteArray();
        }
    }
}
