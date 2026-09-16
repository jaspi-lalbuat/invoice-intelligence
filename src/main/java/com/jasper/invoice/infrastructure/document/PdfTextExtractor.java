package com.jasper.invoice.infrastructure.document;

import com.jasper.invoice.application.document.port.DocumentTextExtractor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

@Component
public class PdfTextExtractor implements DocumentTextExtractor {

    @Override
    public String extract(InputStream inputStream) throws IOException {
        byte[] documentBytes = inputStream.readAllBytes();

        try (PDDocument document = Loader.loadPDF(documentBytes)) {
            PDFTextStripper textStripper = new PDFTextStripper();
            return textStripper.getText(document);
        }
    }
}