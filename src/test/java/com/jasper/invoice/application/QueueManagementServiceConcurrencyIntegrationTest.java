package com.jasper.invoice.application;

import com.jasper.invoice.domain.model.ProcessingStatus;
import com.jasper.invoice.domain.model.InvoiceProcessingJob;
import com.jasper.invoice.domain.repository.InvoiceProcessingJobRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class QueueManagementServiceConcurrencyIntegrationTest {
    @Autowired
    private QueueManagementService queueManagementService;

    @Autowired
    private InvoiceProcessingJobRepository jobRepository;

    @Test
    void shouldAllowOnlyOneWorkerToClaimQueuedJob() throws Exception {
        UUID jobId = UUID.randomUUID();
        String documentReference = "document-" + UUID.randomUUID();

        InvoiceProcessingJob job =
                new InvoiceProcessingJob(jobId, documentReference);

        jobRepository.save(job);

        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            CountDownLatch start = new CountDownLatch(1);

            Future<Optional<ProcessingClaim>> first =
                    executor.submit(() -> {
                        start.await();
                        return queueManagementService.claimJob(jobId);
                    });

            Future<Optional<ProcessingClaim>> second =
                    executor.submit(() -> {
                        start.await();
                        return queueManagementService.claimJob(jobId);
                    });

            start.countDown();

            Optional<ProcessingClaim> firstResult = first.get();
            Optional<ProcessingClaim> secondResult = second.get();

            assertEquals(
                    1,
                    Stream.of(firstResult, secondResult)
                            .filter(Optional::isPresent)
                            .count()
            );

            assertEquals(
                    1,
                    Stream.of(firstResult, secondResult)
                            .filter(Optional::isEmpty)
                            .count()
            );

            InvoiceProcessingJob persistedJob =
                    jobRepository.findById(jobId).orElseThrow();

            assertEquals(ProcessingStatus.PROCESSING, persistedJob.status());
        } finally {
            executor.shutdownNow();
        }
    }
}
