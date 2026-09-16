package com.jasper.invoice.application.deprecated;

import com.jasper.invoice.application.InvoiceProcessingWorker;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Deprecated
@Component
public class AsyncInvoiceProcessingDispatcher
        implements InvoiceProcessingDispatcher {

    private final InvoiceProcessingWorker worker;

    public AsyncInvoiceProcessingDispatcher(
            InvoiceProcessingWorker worker) {
        this.worker = worker;
    }

    @Override
    public void dispatch(UUID jobId) {

    }
}