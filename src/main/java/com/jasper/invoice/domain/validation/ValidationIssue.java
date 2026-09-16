package com.jasper.invoice.domain.validation;

import java.math.BigDecimal;

public record ValidationIssue(
        ValidationIssueCode code,
        String message,
        BigDecimal difference
) {
}