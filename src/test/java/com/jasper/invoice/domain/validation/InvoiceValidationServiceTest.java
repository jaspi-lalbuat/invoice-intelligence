package com.jasper.invoice.domain.validation;

import com.jasper.invoice.domain.model.Invoice;
import com.jasper.invoice.domain.model.InvoiceLineItem;
import com.jasper.invoice.domain.model.Party;
import com.jasper.invoice.domain.model.TaxBreakdown;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InvoiceValidationServiceTest {

    private final InvoiceValidationService service =
            new InvoiceValidationService();

    @Test
    void shouldDetectSubtotalMismatch() {

        Invoice invoice = new Invoice(
                "INV-2026-00124",
                LocalDate.of(2026, 8, 28),
                LocalDate.of(2026, 9, 27),
                "INR",

                new Party(
                        "ABC Technologies Pvt Ltd",
                        null,
                        "29ABCDE1234F1Z5"
                ),

                new Party(
                        "XYZ Solutions Pvt Ltd",
                        null,
                        "27XYZDE5678G1Z2"
                ),

                List.of(
                        new InvoiceLineItem(
                                "MacBook Stand",
                                new BigDecimal("5"),
                                new BigDecimal("2500"),
                                null,
                                null
                        ),
                        new InvoiceLineItem(
                                "USB-C Hub",
                                new BigDecimal("3"),
                                new BigDecimal("4500"),
                                null,
                                null
                        ),
                        new InvoiceLineItem(
                                "Wireless Keyboard",
                                new BigDecimal("2"),
                                new BigDecimal("3000"),
                                null,
                                null
                        )
                ),

                new BigDecimal("34000"),

                new TaxBreakdown(
                        new BigDecimal("3060"),
                        new BigDecimal("3060"),
                        null
                ),

                null,

                new BigDecimal("40120")
        );

        InvoiceValidationResult result = service.validate(invoice);

        assertEquals(
                new BigDecimal("32000"),
                result.calculatedSubtotal()
        );
        assertEquals(
                new BigDecimal("2000"),
                result.subtotalDifference()
        );

        assertEquals(
                new BigDecimal("38120"),
                result.calculatedTotal()
        );

        assertEquals(
                new BigDecimal("2000"),
                result.totalDifference()
        );

        assertEquals(
                ValidationStatus.REVIEW_REQUIRED,
                result.status()
        );

        assertEquals(2, result.issues().size());

        assertEquals(
                ValidationIssueCode.SUBTOTAL_MISMATCH,
                result.issues().get(0).code()
        );

        assertEquals(
                new BigDecimal("2000"),
                result.issues().get(0).difference()
        );

        assertEquals(
                ValidationIssueCode.TOTAL_MISMATCH,
                result.issues().get(1).code()
        );

        assertEquals(
                new BigDecimal("2000"),
                result.issues().get(1).difference()
        );
    }

    @Test
    void shouldAcceptValidInvoice() {

        Invoice invoice = new Invoice(
                "INV-VALID-001",
                LocalDate.of(2026, 8, 28),
                LocalDate.of(2026, 9, 27),
                "INR",

                new Party(
                        "ABC Technologies Pvt Ltd",
                        null,
                        "29ABCDE1234F1Z5"
                ),

                new Party(
                        "XYZ Solutions Pvt Ltd",
                        null,
                        "27XYZDE5678G1Z2"
                ),

                List.of(
                        new InvoiceLineItem(
                                "MacBook Stand",
                                new BigDecimal("5"),
                                new BigDecimal("2500"),
                                null,
                                null
                        ),
                        new InvoiceLineItem(
                                "USB-C Hub",
                                new BigDecimal("3"),
                                new BigDecimal("4500"),
                                null,
                                null
                        ),
                        new InvoiceLineItem(
                                "Wireless Keyboard",
                                new BigDecimal("2"),
                                new BigDecimal("3000"),
                                null,
                                null
                        )
                ),

                new BigDecimal("32000"),

                new TaxBreakdown(
                        new BigDecimal("3060"),
                        new BigDecimal("3060"),
                        null
                ),

                null,

                new BigDecimal("38120")
        );

        InvoiceValidationResult result = service.validate(invoice);
        assertEquals(ValidationStatus.VALID, result.status());
        assertTrue(result.issues().isEmpty());
    }

    @Test
    void shouldRequireReviewWhenRequiredFieldsAreMissing() {

        Invoice invoice = new Invoice(
                null,
                null,
                null,
                "INR",
                null,
                null,
                List.of(),
                null,
                new TaxBreakdown(null, null, null),
                null,
                null
        );

        InvoiceValidationResult result = service.validate(invoice);

        assertEquals(
                ValidationStatus.REVIEW_REQUIRED,
                result.status()
        );

        assertTrue(
                result.issues().stream()
                        .anyMatch(issue ->
                                issue.code() ==
                                        ValidationIssueCode.MISSING_INVOICE_NUMBER)
        );

        assertTrue(
                result.issues().stream()
                        .anyMatch(issue ->
                                issue.code() ==
                                        ValidationIssueCode.MISSING_INVOICE_DATE)
        );

        assertTrue(
                result.issues().stream()
                        .anyMatch(issue ->
                                issue.code() ==
                                        ValidationIssueCode.MISSING_SUPPLIER)
        );

        assertTrue(
                result.issues().stream()
                        .anyMatch(issue ->
                                issue.code() ==
                                        ValidationIssueCode.MISSING_CUSTOMER)
        );

        assertTrue(
                result.issues().stream()
                        .anyMatch(issue ->
                                issue.code() ==
                                        ValidationIssueCode.MISSING_LINE_ITEMS)
        );

        assertTrue(
                result.issues().stream()
                        .anyMatch(issue ->
                                issue.code() ==
                                        ValidationIssueCode.MISSING_TOTAL)
        );
    }

    @Test
    void shouldRequireReviewWhenLineItemDataIsMissing() {

        InvoiceLineItem incompleteItem = new InvoiceLineItem(
                "MacBook Stand",
                null,
                new BigDecimal("2500"),
                null,
                null
        );

        Invoice invoice = new Invoice(
                "INV-INVALID-001",
                LocalDate.of(2026, 8, 28),
                LocalDate.of(2026, 9, 27),
                "INR",

                new Party(
                        "ABC Technologies Pvt Ltd",
                        null,
                        "29ABCDE1234F1Z5"
                ),

                new Party(
                        "XYZ Solutions Pvt Ltd",
                        null,
                        "27XYZDE5678G1Z2"
                ),

                List.of(incompleteItem),

                new BigDecimal("12500"),

                new TaxBreakdown(
                        new BigDecimal("1000"),
                        new BigDecimal("1000"),
                        null
                ),

                null,

                new BigDecimal("14500")
        );

        InvoiceValidationResult result = service.validate(invoice);

        assertEquals(
                ValidationStatus.REVIEW_REQUIRED,
                result.status()
        );

        assertTrue(
                result.issues().stream()
                        .anyMatch(issue ->
                                issue.code() ==
                                        ValidationIssueCode.MISSING_LINE_ITEM_QUANTITY)
        );
    }

    @Test
    void shouldTreatMissingTaxesAsZero() {

        Invoice invoice = new Invoice(
                "INV-NO-TAXES",
                LocalDate.of(2026, 9, 1),
                null,
                "INR",
                new Party("Supplier", null, null),
                new Party("Customer", null, null),
                List.of(new InvoiceLineItem(
                        "Service",
                        BigDecimal.ONE,
                        new BigDecimal("100"),
                        null,
                        new BigDecimal("100")
                )),
                new BigDecimal("100"),
                null,
                null,
                new BigDecimal("100")
        );

        InvoiceValidationResult result = service.validate(invoice);

        assertEquals(ValidationStatus.VALID, result.status());
    }
}
