package com.jasper.invoice.infrastructure.extraction;

import com.jasper.invoice.application.document.port.InvoiceExtractor;
import com.jasper.invoice.domain.model.Invoice;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Tag("ollama")
@SpringBootTest
class OllamaInvoiceExtractorIntegrationTest {

    @Autowired
    private InvoiceExtractor invoiceExtractor;

    @Test
    void shouldExtractInvoiceFromDocumentText() {

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

        Invoice invoice =
                invoiceExtractor.extract(documentText);

        assertNotNull(invoice);

        assertEquals("INV-1001", invoice.invoiceNumber());
        assertEquals("INR", invoice.currency());

        assertNotNull(invoice.supplier());
        assertEquals(
                "ABC Technologies Pvt Ltd",
                invoice.supplier().name()
        );

        assertNotNull(invoice.customer());
        assertEquals(
                "XYZ Solutions Pvt Ltd",
                invoice.customer().name()
        );

        assertEquals(2, invoice.lineItems().size());

        assertEquals(
                0,
                invoice.subtotal().compareTo(
                        new java.math.BigDecimal("145000")
                )
        );

        assertEquals(
                0,
                invoice.total().compareTo(
                        new java.math.BigDecimal("171100")
                )
        );
    }
}