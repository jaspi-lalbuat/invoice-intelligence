package com.jasper.invoice.domain.model;

import java.math.BigDecimal;

public record InvoiceLineItem(
        String description,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal taxRate,
        BigDecimal statedTotal
) {}