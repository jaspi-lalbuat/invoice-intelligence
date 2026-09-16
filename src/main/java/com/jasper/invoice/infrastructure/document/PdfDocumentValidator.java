package com.jasper.invoice.infrastructure.document;

import com.jasper.invoice.application.document.port.DocumentValidator;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

@Component
public class PdfDocumentValidator implements DocumentValidator {

    @Override
    public void validate(InputStream inputStream) throws IOException {

        byte[] documentBytes = inputStream.readAllBytes();

        try (PDDocument ignored = Loader.loadPDF(documentBytes)) {
            // Successfully parsed as a PDF.
        }
    }
}