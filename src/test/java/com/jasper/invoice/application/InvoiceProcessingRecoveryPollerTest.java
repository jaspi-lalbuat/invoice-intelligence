package com.jasper.invoice.application;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class InvoiceProcessingRecoveryPollerTest {

    @Test
    void shouldRecoverExpiredJobs() {
        QueueManagementService queueManagementService =
                mock(QueueManagementService.class);

        InvoiceProcessingRecoveryPoller poller =
                new InvoiceProcessingRecoveryPoller(
                        queueManagementService
                );

        poller.recover();

        verify(queueManagementService)
                .recoverExpiredJobs();
    }
}