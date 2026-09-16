package com.jasper.invoice.application;

import com.jasper.invoice.domain.model.ProcessingStatus;
import com.jasper.invoice.domain.model.InvoiceProcessingJob;
import com.jasper.invoice.domain.repository.InvoiceProcessingJobRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@SpringBootTest
class ProcessingRecoveryIntegrationTest {

    @Autowired
    private QueueManagementService queueManagementService;

    @Autowired
    private InvoiceProcessingJobRepository repository;

    @Test
    void shouldRequeueExpiredProcessingJob() {
        UUID jobId = UUID.randomUUID();

        InvoiceProcessingJob job =
                new InvoiceProcessingJob(
                        jobId,
                        "document-" + UUID.randomUUID(),
                        "invoice.pdf"
                );

        repository.save(job);

        Instant expiredLease = Instant.now().minusSeconds(60);

        job.startProcessing(expiredLease);
        repository.save(job);

        int recovered = queueManagementService.recoverExpiredJobs();

        assertEquals(1, recovered);

        InvoiceProcessingJob recoveredJob =
                repository.findById(jobId).orElseThrow();

        assertEquals(ProcessingStatus.QUEUED, recoveredJob.status());
        assertNull(recoveredJob.leaseUntil());
    }
}
