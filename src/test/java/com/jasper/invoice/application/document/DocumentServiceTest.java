package com.jasper.invoice.application.document;

import com.jasper.invoice.application.document.port.DocumentTextExtractor;
import com.jasper.invoice.application.document.port.DocumentTextNormalizer;
import com.jasper.invoice.application.document.port.InvoiceExtractor;
import com.jasper.invoice.domain.model.Invoice;
import com.jasper.invoice.domain.validation.InvoiceValidationResult;
import com.jasper.invoice.domain.validation.InvoiceValidationService;
import com.jasper.invoice.domain.validation.ValidationStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private DocumentTextExtractor textExtractor;

    @Mock
    private DocumentTextNormalizer textNormalizer;

    @Mock
    private InvoiceExtractor invoiceExtractor;

    @Mock
    private InvoiceValidationService invoiceValidationService;

    @InjectMocks
    private DocumentService documentService;

    @Test
    void shouldExtractAndValidateInvoice() throws Exception {

        InputStream inputStream =
                new ByteArrayInputStream(
                        "raw pdf content".getBytes()
                );

        String rawText = "  Invoice Number: INV-001  ";
        String normalizedText = "Invoice Number: INV-001";

        Invoice invoice = mock(Invoice.class);

        InvoiceValidationResult validation =
                new InvoiceValidationResult(
                        ValidationStatus.VALID,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        List.of()
                );

        when(textExtractor.extract(inputStream))
                .thenReturn(rawText);

        when(textNormalizer.normalize(rawText))
                .thenReturn(normalizedText);

        when(invoiceExtractor.extract(normalizedText))
                .thenReturn(invoice);

        when(invoiceValidationService.validate(invoice))
                .thenReturn(validation);

        InvoiceExtractionResponse result =
                documentService.extractInvoice(inputStream);

        assertSame(invoice, result.invoice());
        assertSame(validation, result.validation());

        verify(textExtractor).extract(inputStream);
        verify(textNormalizer).normalize(rawText);
        verify(invoiceExtractor).extract(normalizedText);
        verify(invoiceValidationService).validate(invoice);
    }

    @Test
    void shouldPropagateTextExtractionFailure() throws Exception {

        InputStream inputStream =
                new ByteArrayInputStream(
                        "invalid".getBytes()
                );

        IOException failure =
                new IOException("Failed to extract text");

        when(textExtractor.extract(inputStream))
                .thenThrow(failure);

        IOException thrown =
                assertThrows(
                        IOException.class,
                        () -> documentService.extractInvoice(inputStream)
                );

        assertSame(failure, thrown);

        verify(textExtractor).extract(inputStream);
        verifyNoInteractions(
                textNormalizer,
                invoiceExtractor,
                invoiceValidationService
        );
    }
}