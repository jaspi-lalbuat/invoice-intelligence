package com.jasper.invoice.application.port;

import com.jasper.invoice.application.InvoiceProcessingRequested;

public interface InvoiceProcessingEventPublisher {

    void publish(InvoiceProcessingRequested event);
}