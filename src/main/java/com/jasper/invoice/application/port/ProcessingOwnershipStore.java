package com.jasper.invoice.application.port;

import com.jasper.invoice.application.ProcessingClaim;
import com.jasper.invoice.application.ProcessingFailureResult;

public interface ProcessingOwnershipStore {

    void persistClaim(ProcessingClaim claim);

    boolean complete(ProcessingClaim claim);

    ProcessingFailureResult retryOrFail(ProcessingClaim claim);
}