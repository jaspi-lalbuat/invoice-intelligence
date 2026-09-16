package com.jasper.invoice.domain.model;

import java.math.BigDecimal;

public record TaxBreakdown(
        BigDecimal cgst,
        BigDecimal sgst,
        BigDecimal igst
) {}