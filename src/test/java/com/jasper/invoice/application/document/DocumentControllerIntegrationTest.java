package com.jasper.invoice.application.document;

import com.jasper.invoice.domain.model.ProcessingStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jasper.invoice.application.port.InvoiceProcessingEventPublisher;
import com.jasper.invoice.domain.model.*;
import com.jasper.invoice.domain.repository.InvoiceProcessingJobRepository;
import com.jasper.invoice.domain.validation.InvoiceValidationResult;
import com.jasper.invoice.domain.validation.ValidationIssue;
import com.jasper.invoice.domain.validation.ValidationIssueCode;
import com.jasper.invoice.domain.validation.ValidationStatus;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DocumentControllerIntegrationTest {

    @MockitoBean
    private InvoiceProcessingEventPublisher eventPublisher;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InvoiceProcessingJobRepository repository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void cleanDatabase() {
        // Use your existing cleanup approach.
    }

    @Test
    void shouldRetryFailedJob() throws Exception {
        UUID jobId = UUID.randomUUID();

        InvoiceProcessingJob job =
                new InvoiceProcessingJob(
                        jobId,
                        "invoice.pdf",
                        "document-" + UUID.randomUUID()
                );

        job.startProcessing(
                Instant.now().plusSeconds(120)
        );
        job.fail();

        repository.save(job);

        mockMvc.perform(
                post("/api/v1/documents/{jobId}/retry", jobId)
        )
        .andExpect(status().isAccepted());

        InvoiceProcessingJob saved =
                repository.findById(jobId).orElseThrow();

        assertEquals(
                ProcessingStatus.QUEUED,
                saved.status()
        );

        assertEquals(1, saved.retryCount());
    }

    @Test
    void shouldReturn404WhenRetryingNonexistentJob() throws Exception {
        UUID jobId = UUID.randomUUID();

        mockMvc.perform(
                        post("/api/v1/documents/{jobId}/retry", jobId)
                )
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn409WhenRetryingNonFailedJob() throws Exception {
        UUID jobId = UUID.randomUUID();

        InvoiceProcessingJob job =
                new InvoiceProcessingJob(
                        jobId,
                        "invoice.pdf",
                        "document-" + UUID.randomUUID()
                );

        repository.save(job);

        mockMvc.perform(
                        post("/api/v1/documents/{jobId}/retry", jobId)
                )
                .andExpect(status().isConflict());
    }

    @Test
    void shouldCreateQueuedJobFromUploadedDocument() throws Exception {

        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "invoice.pdf",
                        "application/pdf",
                        createValidPdf()
                );

        String response =
                mockMvc.perform(
                                multipart("/api/v1/documents")
                                        .file(file)
                        )
                        .andExpect(status().isAccepted())
                        .andExpect(jsonPath("$.jobId").exists())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        UUID jobId = UUID.fromString(
                objectMapper.readTree(response)
                        .get("jobId")
                        .asText()
        );

        InvoiceProcessingJob saved =
                repository.findById(jobId).orElseThrow();

        assertEquals(
                ProcessingStatus.QUEUED,
                saved.status()
        );
    }

    @Test
    void shouldReturn400ForEmptyFile() throws Exception {

        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "invoice.pdf",
                        "application/pdf",
                        new byte[0]
                );

        mockMvc.perform(
                        multipart("/api/v1/documents")
                                .file(file)
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400ForUnsupportedFileType() throws Exception {

        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "invoice.txt",
                        "text/plain",
                        "invoice".getBytes()
                );

        mockMvc.perform(
                        multipart("/api/v1/documents")
                                .file(file)
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400ForMalformedPdf() throws Exception {

        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "invoice.pdf",
                        "application/pdf",
                        "this is not a real pdf".getBytes()
                );

        mockMvc.perform(
                        multipart("/api/v1/documents")
                                .file(file)
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnProcessingJob() throws Exception {
        UUID jobId = UUID.randomUUID();

        InvoiceProcessingJob job =
                new InvoiceProcessingJob(
                        jobId,
                        "invoice.pdf",
                        "document-" + UUID.randomUUID()
                );

        repository.save(job);

        mockMvc.perform(
                        get("/api/v1/documents/{jobId}", jobId)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").value(jobId.toString()))
                .andExpect(jsonPath("$.status").value("QUEUED"))
                .andExpect(jsonPath("$.invoice").doesNotExist())
                .andExpect(jsonPath("$.validation").doesNotExist())
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());
    }

    @Test
    void shouldReturn404WhenGettingNonexistentJob() throws Exception {
        UUID jobId = UUID.randomUUID();

        mockMvc.perform(
                        get("/api/v1/documents/{jobId}", jobId)
                )
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnReadyProcessingJob() throws Exception {
        UUID jobId = UUID.randomUUID();

        InvoiceProcessingJob job =
                new InvoiceProcessingJob(jobId, "invoice.pdf", "document-" + UUID.randomUUID());

        Invoice invoice = createInvoice();

        InvoiceValidationResult validation =
                new InvoiceValidationResult(
                        ValidationStatus.VALID,
                        new BigDecimal("1000.00"),
                        BigDecimal.ZERO,
                        new BigDecimal("1180.00"),
                        BigDecimal.ZERO,
                        List.of()
                );

        job.startProcessing(Instant.now().plusSeconds(120));
        job.complete(invoice, validation);

        repository.save(job);

        mockMvc.perform(
                        get("/api/v1/documents/{jobId}", jobId)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").value(jobId.toString()))
                .andExpect(jsonPath("$.status").value("READY"))
                .andExpect(jsonPath("$.invoice.invoiceNumber").value("INV-001"))
                .andExpect(jsonPath("$.invoice.currency").value("INR"))
                .andExpect(jsonPath("$.validation.status").value("VALID"))
                .andExpect(jsonPath("$.validation.calculatedSubtotal").value(1000.00))
                .andExpect(jsonPath("$.validation.subtotalDifference").value(0))
                .andExpect(jsonPath("$.validation.calculatedTotal").value(1180.00))
                .andExpect(jsonPath("$.validation.totalDifference").value(0));
    }

    @Test
    void shouldReturnReviewRequiredProcessingJob() throws Exception {
        UUID jobId = UUID.randomUUID();

        InvoiceProcessingJob job =
                new InvoiceProcessingJob(jobId, "invoice.pdf", "document-" + UUID.randomUUID());

        Invoice invoice = createInvoice();

        InvoiceValidationResult validation =
                new InvoiceValidationResult(
                        ValidationStatus.REVIEW_REQUIRED,
                        new BigDecimal("1000.00"),
                        new BigDecimal("-100.00"),
                        new BigDecimal("1180.00"),
                        new BigDecimal("-100.00"),
                        List.of(
                                new ValidationIssue(
                                        ValidationIssueCode.SUBTOTAL_MISMATCH,
                                        "Subtotal does not match calculated line item total",
                                        new BigDecimal("-100.00")
                                )
                        )
                );

        job.startProcessing(Instant.now().plusSeconds(120));
        job.complete(invoice, validation);

        repository.save(job);

        mockMvc.perform(
                        get("/api/v1/documents/{jobId}", jobId)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").value(jobId.toString()))
                .andExpect(jsonPath("$.status").value("REVIEW_REQUIRED"))
                .andExpect(jsonPath("$.invoice.invoiceNumber").value("INV-001"))
                .andExpect(jsonPath("$.validation.status").value("REVIEW_REQUIRED"))
                .andExpect(jsonPath("$.validation.issues[0].code")
                        .value("SUBTOTAL_MISMATCH"))
                .andExpect(jsonPath("$.validation.issues[0].difference")
                        .value(-100.00));
    }

    @Test
    void shouldReturnFailedProcessingJob() throws Exception {
        UUID jobId = UUID.randomUUID();

        InvoiceProcessingJob job =
                new InvoiceProcessingJob(jobId, "invoice.pdf", "document-" + UUID.randomUUID());

        job.startProcessing(Instant.now().plusSeconds(120));
        job.fail();

        repository.save(job);

        mockMvc.perform(
                        get("/api/v1/documents/{jobId}", jobId)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").value(jobId.toString()))
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.invoice").doesNotExist())
                .andExpect(jsonPath("$.validation").doesNotExist());
    }

    private byte[] createValidPdf() throws IOException {

        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {

            document.addPage(new PDPage());
            document.save(output);

            return output.toByteArray();
        }
    }

    private Invoice createInvoice() {
        return new Invoice(
                "INV-001",
                LocalDate.of(2026, 9, 15),
                LocalDate.of(2026, 9, 30),
                "INR",
                new Party(
                        "Supplier Ltd",
                        "Supplier Address",
                        "GSTIN-SUPPLIER"
                ),
                new Party(
                        "Customer Ltd",
                        "Customer Address",
                        "GSTIN-CUSTOMER"
                ),
                List.of(
                        new InvoiceLineItem(
                                "Software Service",
                                new BigDecimal("1"),
                                new BigDecimal("1000.00"),
                                new BigDecimal("18"),
                                null
                        )
                ),
                new BigDecimal("1000.00"),
                new TaxBreakdown(
                        new BigDecimal("90.00"),
                        new BigDecimal("90.00"),
                        BigDecimal.ZERO
                ),
                BigDecimal.ZERO,
                new BigDecimal("1180.00")
        );
    }
}
