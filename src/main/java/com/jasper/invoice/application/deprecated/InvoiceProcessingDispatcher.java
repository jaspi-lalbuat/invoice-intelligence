package com.jasper.invoice.application.deprecated;

import java.util.UUID;

@Deprecated
public interface InvoiceProcessingDispatcher {

    void dispatch(UUID jobId);
}