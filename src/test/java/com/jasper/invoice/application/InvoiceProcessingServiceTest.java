package com.jasper.invoice.application;

import com.jasper.invoice.application.document.port.DocumentValidator;
import com.jasper.invoice.application.document.port.DocumentStorage;
import com.jasper.invoice.domain.model.InvoiceProcessingJob;
import com.jasper.invoice.domain.repository.InvoiceProcessingJobRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceProcessingServiceTest {

    @Mock
    private InvoiceProcessingJobRepository repository;
    @Mock
    private DocumentStorage documentStorage;
    @Mock
    private DocumentValidator documentValidator;
    @Mock
    private InvoiceJobCreationService jobCreationService;

    @InjectMocks
    private InvoiceProcessingService service;

    @Test
    void shouldCreateQueuedJob() {
        String documentReference =
                "placeholder-" + UUID.randomUUID();

        InvoiceProcessingJob expectedJob =
                new InvoiceProcessingJob(
                        UUID.randomUUID(),
                        documentReference
                );

        when(jobCreationService.createJob(documentReference))
                .thenReturn(expectedJob);

        InvoiceProcessingJob job =
                service.createJob(documentReference);

        assertSame(expectedJob, job);

        verify(jobCreationService)
                .createJob(documentReference);
    }


    @Test
    void shouldDeleteDocumentWhenJobCreationFails() throws Exception {

        String documentReference = "document-" + UUID.randomUUID();

        MultipartFile file =
                new MockMultipartFile(
                        "file",
                        "invoice.pdf",
                        "application/pdf",
                        "content".getBytes()
                );

        when(documentStorage.store(file))
                .thenReturn(documentReference);

        RuntimeException failure =
                new RuntimeException("database failure");
        when(jobCreationService.createJob(documentReference))
                .thenThrow(failure);

        RuntimeException thrown =
                assertThrows(
                        RuntimeException.class,
                        () -> service.submitInvoice(file)
                );

        assertEquals(failure, thrown);

        verify(documentStorage).store(file);
        verify(documentStorage).delete(documentReference);
    }

    @Test
    void shouldRejectEmptyFile() {

        MultipartFile file =
                new MockMultipartFile(
                        "file",
                        "invoice.pdf",
                        "application/pdf",
                        new byte[0]
                );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> service.submitInvoice(file)
                );

        assertEquals("Uploaded file is empty", exception.getMessage());

        verifyNoInteractions(documentStorage);
        verifyNoInteractions(repository);
    }

    @Test
    void shouldRejectUnsupportedFileType() {

        MultipartFile file =
                new MockMultipartFile(
                        "file",
                        "invoice.txt",
                        "text/plain",
                        "invoice".getBytes()
                );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> service.submitInvoice(file)
                );

        assertEquals(
                "Only PDF files are supported",
                exception.getMessage()
        );

        verifyNoInteractions(documentStorage);
        verifyNoInteractions(repository);
    }
}