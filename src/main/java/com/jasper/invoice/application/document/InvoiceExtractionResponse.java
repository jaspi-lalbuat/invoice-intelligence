package com.jasper.invoice.application.document;

import com.jasper.invoice.domain.model.Invoice;
import com.jasper.invoice.domain.validation.InvoiceValidationResult;

public record InvoiceExtractionResponse(
        Invoice invoice,
        InvoiceValidationResult validation
) {
}