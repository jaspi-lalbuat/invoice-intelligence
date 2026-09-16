package com.jasper.invoice.application.document;

import com.jasper.invoice.application.InvoiceProcessingJobNotFoundException;
import com.jasper.invoice.application.InvoiceProcessingService;
import com.jasper.invoice.domain.model.InvalidProcessingStateException;
import com.jasper.invoice.domain.model.InvoiceProcessingJob;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@AllArgsConstructor
@RestController
@RequestMapping("/api/v1/documents")
public class DocumentController {

    private final DocumentService documentService;
    private final InvoiceProcessingService processingService;

    @PostMapping(
            value = "/extract-text",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public String extractText(@RequestParam("file") MultipartFile file)
            throws IOException {

        return documentService.extractText(file.getInputStream());
    }

    @PostMapping(
            value = "/extract-invoice",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public InvoiceExtractionResponse extractInvoice(@RequestParam("file") MultipartFile file)
            throws IOException {

        return documentService.extractInvoice(file.getInputStream());
    }

    @PostMapping(
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<CreateInvoiceProcessingJobResponse> createJob(
            @RequestParam("file") MultipartFile file) throws IOException {

        InvoiceProcessingJob job =
                processingService.submitInvoice(file);

        return ResponseEntity.accepted()
                .body(new CreateInvoiceProcessingJobResponse(job.id()));
    }

//    @PostMapping("/{jobId}/process")
//    public ResponseEntity<Void> processJob(
//            @PathVariable UUID jobId) {
//
//        processingWorker.process(jobId);
//
//        return ResponseEntity.ok().build();
//    }

    @GetMapping("/{jobId}")
    public InvoiceProcessingJobResponse getJob(
            @PathVariable UUID jobId) {

        InvoiceProcessingJob job =
                processingService.getJob(jobId);

        return new InvoiceProcessingJobResponse(
                job.id(),
                job.status(),
                job.invoice(),
                job.validationResult(),
                job.createdAt(),
                job.updatedAt()
        );
    }

    @PostMapping("/{jobId}/retry")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void retry(
            @PathVariable UUID jobId
    ) {
        processingService.retryJob(jobId);
    }

    @ExceptionHandler(InvoiceProcessingJobNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public void handleJobNotFound() {
    }

    @ExceptionHandler(InvalidProcessingStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public void handleInvalidProcessingState() {
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public void handleBadRequest() {
    }

    @ExceptionHandler(IOException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public void handleInvalidDocument() {
    }
}
