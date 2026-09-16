package com.jasper.invoice.infrastructure.extraction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record OllamaInvoiceResponse(
        String invoiceNumber,
        LocalDate invoiceDate,
        LocalDate dueDate,
        String currency,
        PartyResponse supplier,
        PartyResponse customer,
        List<InvoiceLineItemResponse> lineItems,
        BigDecimal subtotal,
        TaxBreakdownResponse taxes,
        BigDecimal discount,
        BigDecimal total
) {

    public record InvoiceLineItemResponse(
            String description,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal taxRate,
            BigDecimal statedTotal
    ) {}

    public record TaxBreakdownResponse(
            BigDecimal cgst,
            BigDecimal sgst,
            BigDecimal igst
    ) {}

    public record PartyResponse(
            String name,
            String address,
            String taxId
    ) {}
}