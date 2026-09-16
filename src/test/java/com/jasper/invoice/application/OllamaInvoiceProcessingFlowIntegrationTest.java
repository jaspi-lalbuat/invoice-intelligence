package com.jasper.invoice.application;

import com.jasper.invoice.domain.model.ProcessingStatus;
import com.jasper.invoice.domain.model.InvoiceProcessingJob;
import com.jasper.invoice.domain.repository.InvoiceProcessingJobRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("ollama")
@SpringBootTest(
        properties = "spring.task.scheduling.enabled=false"
)
class OllamaInvoiceProcessingFlowIntegrationTest {

    @Autowired
    private InvoiceProcessingService processingService;

    @Autowired
    private InvoiceProcessingPoller poller;

    @Autowired
    private InvoiceProcessingJobRepository repository;

    @BeforeEach
    void cleanDatabase() throws IOException {
        // We'll add DB/storage cleanup here.
    }

    @Test
    void shouldProcessInvoiceUsingRealOllamaPipeline() throws Exception {

        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "invoice.pdf",
                        "application/pdf",
                        createInvoicePdf()
                );

        InvoiceProcessingJob created =
                processingService.submitInvoice(file);

        assertEquals(
                ProcessingStatus.QUEUED,
                created.status()
        );

        poller.poll();

        InvoiceProcessingJob processed =
                repository.findById(created.id())
                        .orElseThrow();

        assertEquals(
                ProcessingStatus.READY,
                processed.status()
        );

        assertEquals(
                "INV-1001",
                processed.invoice().invoiceNumber()
        );

        assertEquals(
                "ABC Technologies Pvt Ltd",
                processed.invoice().supplier().name()
        );

        assertEquals(
                "XYZ Solutions Pvt Ltd",
                processed.invoice().customer().name()
        );

        assertEquals(
                2,
                processed.invoice().lineItems().size()
        );
    }

    private byte[] createInvoicePdf() throws IOException {

        String documentText = """
            TAX INVOICE

            Invoice Number: INV-1001
            Invoice Date: 2026-09-10
            Currency: INR

            Supplier:
            ABC Technologies Pvt Ltd
            GSTIN: 27ABCDE1234F1Z5

            Customer:
            XYZ Solutions Pvt Ltd
            GSTIN: 29XYZDE5678G1Z2

            Description       Qty    Unit Price
            Laptop            2      50000
            Monitor            3      15000

            Subtotal: 145000
            CGST: 13050
            SGST: 13050
            Total: 171100
            """;

        try (
                PDDocument document = new PDDocument();
                ByteArrayOutputStream output = new ByteArrayOutputStream()
        ) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream content =
                         new PDPageContentStream(document, page)) {

                content.beginText();
                content.setFont(
                        new PDType1Font(Standard14Fonts.FontName.HELVETICA),
                        10
                );
                content.newLineAtOffset(50, 750);

                for (String line : documentText.split("\n")) {
                    content.showText(line);
                    content.newLineAtOffset(0, -15);
                }

                content.endText();
            }

            document.save(output);
            return output.toByteArray();
        }
    }
}
