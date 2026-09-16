package com.jasper.invoice.application.document;

import com.jasper.invoice.application.document.port.DocumentTextExtractor;
import com.jasper.invoice.application.document.port.DocumentTextNormalizer;
import com.jasper.invoice.domain.model.Invoice;
import com.jasper.invoice.application.document.port.InvoiceExtractor;
import com.jasper.invoice.domain.validation.InvoiceValidationResult;
import com.jasper.invoice.domain.validation.InvoiceValidationService;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;

@Service
public class DocumentService {

    private final DocumentTextExtractor textExtractor;
    private final DocumentTextNormalizer textNormalizer;
    private final InvoiceExtractor invoiceExtractor;
    private final InvoiceValidationService invoiceValidationService;

    public DocumentService(
            DocumentTextExtractor textExtractor,
            DocumentTextNormalizer textNormalizer,
            InvoiceExtractor invoiceExtractor, InvoiceValidationService invoiceValidationService
    ) {
        this.textExtractor = textExtractor;
        this.textNormalizer = textNormalizer;
        this.invoiceExtractor = invoiceExtractor;
        this.invoiceValidationService = invoiceValidationService;
    }

    public String extractText(InputStream inputStream) throws IOException {
        String rawText = textExtractor.extract(inputStream);
        return textNormalizer.normalize(rawText);
    }

    public InvoiceExtractionResponse extractInvoice(InputStream inputStream)
            throws IOException {

        String text = extractText(inputStream);

        Invoice invoice = invoiceExtractor.extract(text);

        InvoiceValidationResult validation =
                invoiceValidationService.validate(invoice);

        return new InvoiceExtractionResponse(invoice, validation);
    }
}