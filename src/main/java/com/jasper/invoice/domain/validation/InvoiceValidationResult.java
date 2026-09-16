package com.jasper.invoice.domain.validation;

import java.math.BigDecimal;
import java.util.List;

public record InvoiceValidationResult(
        ValidationStatus status,
        BigDecimal calculatedSubtotal,
        BigDecimal subtotalDifference,
        BigDecimal calculatedTotal,
        BigDecimal totalDifference,
        List<ValidationIssue> issues
) {
}