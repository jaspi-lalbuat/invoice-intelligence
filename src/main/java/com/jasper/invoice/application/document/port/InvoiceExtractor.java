package com.jasper.invoice.application.document.port;


import com.jasper.invoice.domain.model.Invoice;

public interface InvoiceExtractor {

    Invoice extract(String documentText);
}