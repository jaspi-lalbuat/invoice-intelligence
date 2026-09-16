package com.jasper.invoice.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record Invoice(
        String invoiceNumber,
        LocalDate invoiceDate,
        LocalDate dueDate,
        String currency,
        Party supplier,
        Party customer,
        List<InvoiceLineItem> lineItems,
        BigDecimal subtotal,
        TaxBreakdown taxes,
        BigDecimal discount,
        BigDecimal total
) {}