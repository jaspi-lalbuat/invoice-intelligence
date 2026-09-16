package com.jasper.invoice.application;

import com.jasper.invoice.application.document.port.DocumentValidator;
import com.jasper.invoice.application.document.port.DocumentStorage;
import com.jasper.invoice.domain.model.InvoiceProcessingJob;
import com.jasper.invoice.domain.repository.InvoiceProcessingJobRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
@AllArgsConstructor
public class InvoiceProcessingService {

    private final InvoiceProcessingJobRepository repository;
    private final QueueManagementService queueManagementService;
    private final DocumentStorage documentStorage;
    private final DocumentValidator documentValidator;
    private final InvoiceJobCreationService jobCreationService;

    public InvoiceProcessingJob submitInvoice(MultipartFile file)
            throws IOException {

        if (file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is empty");
        }

        if (!"application/pdf".equalsIgnoreCase(file.getContentType())) {
            throw new IllegalArgumentException("Only PDF files are supported");
        }

        documentValidator.validate(file.getInputStream());

        String documentReference =
                documentStorage.store(file);

        try {
            return createJob(documentReference);
        } catch (RuntimeException e) {
            try {
                documentStorage.delete(documentReference);
            } catch (IOException cleanupException) {
                e.addSuppressed(cleanupException);
            }
            throw e;
        }
    }

    public InvoiceProcessingJob createJob(String documentReference) {
        return jobCreationService.createJob(documentReference);
    }

    public InvoiceProcessingJob getJob(UUID jobId) {

        return repository.findById(jobId)
                .orElseThrow(() -> new InvoiceProcessingJobNotFoundException(jobId));
    }

    public void retryJob(UUID jobId) {
        queueManagementService.retryJob(jobId);
    }
}