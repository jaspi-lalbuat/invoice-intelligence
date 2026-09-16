package com.jasper.invoice.domain.validation;

import com.jasper.invoice.domain.model.Invoice;
import com.jasper.invoice.domain.model.InvoiceLineItem;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.apache.logging.log4j.util.Strings.isBlank;

@Service
public class InvoiceValidationService {

    public InvoiceValidationResult validate(Invoice invoice) {

        List<ValidationIssue> issues = new ArrayList<>();
        if (invoice.lineItems() != null) {
            validateLineItems(invoice.lineItems(), issues);
        }

        validateRequiredFields(invoice, issues);

        BigDecimal calculatedSubtotal = BigDecimal.ZERO;
        BigDecimal subtotalDifference = null;

        if (invoice.subtotal() != null &&
                invoice.lineItems() != null &&
                !invoice.lineItems().isEmpty()) {

            calculatedSubtotal = invoice.lineItems().stream()
                    .filter(item ->
                            item.quantity() != null &&
                                    item.unitPrice() != null
                    )
                    .map(this::calculateLineTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            subtotalDifference = invoice.subtotal()
                    .subtract(calculatedSubtotal)
                    .abs();

            if (subtotalDifference.compareTo(BigDecimal.ZERO) != 0) {
                issues.add(new ValidationIssue(
                        ValidationIssueCode.SUBTOTAL_MISMATCH,
                        "Calculated subtotal " + calculatedSubtotal
                                + " does not match stated subtotal "
                                + invoice.subtotal(),
                        subtotalDifference
                ));
            }
        }

        BigDecimal calculatedTotal = null;
        BigDecimal totalDifference = null;

        if (invoice.total() != null) {

            BigDecimal calculatedTax = calculateTax(invoice);

            calculatedTotal = calculatedSubtotal
                    .add(calculatedTax)
                    .subtract(nullToZero(invoice.discount()));

            totalDifference = invoice.total()
                    .subtract(calculatedTotal)
                    .abs();

            if (totalDifference.compareTo(BigDecimal.ZERO) != 0) {
                issues.add(new ValidationIssue(
                        ValidationIssueCode.TOTAL_MISMATCH,
                        "Calculated total " + calculatedTotal
                                + " does not match stated total "
                                + invoice.total(),
                        totalDifference
                ));
            }
        }

        ValidationStatus status = issues.isEmpty()
                ? ValidationStatus.VALID
                : ValidationStatus.REVIEW_REQUIRED;

        return new InvoiceValidationResult(
                status,
                calculatedSubtotal,
                subtotalDifference,
                calculatedTotal,
                totalDifference,
                issues
        );
    }

    private void validateRequiredFields(
            Invoice invoice,
            List<ValidationIssue> issues) {

        if (isBlank(invoice.invoiceNumber())) {
            issues.add(new ValidationIssue(
                    ValidationIssueCode.MISSING_INVOICE_NUMBER,
                    "Invoice number is missing",
                    null
            ));
        }

        if (invoice.invoiceDate() == null) {
            issues.add(new ValidationIssue(
                    ValidationIssueCode.MISSING_INVOICE_DATE,
                    "Invoice date is missing",
                    null
            ));
        }

        if (invoice.supplier() == null ||
                isBlank(invoice.supplier().name())) {

            issues.add(new ValidationIssue(
                    ValidationIssueCode.MISSING_SUPPLIER,
                    "Supplier information is missing",
                    null
            ));
        }

        if (invoice.customer() == null ||
                isBlank(invoice.customer().name())) {

            issues.add(new ValidationIssue(
                    ValidationIssueCode.MISSING_CUSTOMER,
                    "Customer information is missing",
                    null
            ));
        }

        if (invoice.lineItems() == null ||
                invoice.lineItems().isEmpty()) {

            issues.add(new ValidationIssue(
                    ValidationIssueCode.MISSING_LINE_ITEMS,
                    "Invoice contains no line items",
                    null
            ));
        }

        if (invoice.total() == null) {
            issues.add(new ValidationIssue(
                    ValidationIssueCode.MISSING_TOTAL,
                    "Invoice total is missing",
                    null
            ));
        }
    }

    private void validateLineItems(
            List<InvoiceLineItem> lineItems,
            List<ValidationIssue> issues) {

        for (int i = 0; i < lineItems.size(); i++) {

            InvoiceLineItem item = lineItems.get(i);

            if (isBlank(item.description())) {
                issues.add(new ValidationIssue(
                        ValidationIssueCode.MISSING_LINE_ITEM_DESCRIPTION,
                        "Line item " + (i + 1) + " is missing a description",
                        null
                ));
            }

            if (item.quantity() == null) {
                issues.add(new ValidationIssue(
                        ValidationIssueCode.MISSING_LINE_ITEM_QUANTITY,
                        "Line item " + (i + 1) + " is missing quantity",
                        null
                ));
            }

            if (item.unitPrice() == null) {
                issues.add(new ValidationIssue(
                        ValidationIssueCode.MISSING_LINE_ITEM_UNIT_PRICE,
                        "Line item " + (i + 1) + " is missing unit price",
                        null
                ));
            }
        }
    }

    private BigDecimal calculateLineTotal(InvoiceLineItem item) {
        return item.quantity().multiply(item.unitPrice());
    }

    private BigDecimal calculateTax(Invoice invoice) {
        if (invoice.taxes() == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal cgst = nullToZero(invoice.taxes().cgst());
        BigDecimal sgst = nullToZero(invoice.taxes().sgst());
        BigDecimal igst = nullToZero(invoice.taxes().igst());

        return cgst.add(sgst).add(igst);
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
