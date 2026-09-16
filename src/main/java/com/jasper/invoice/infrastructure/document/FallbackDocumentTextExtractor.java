package com.jasper.invoice.infrastructure.document;

import com.jasper.invoice.application.document.port.DocumentTextExtractor;
import com.jasper.invoice.application.document.port.ImageTextExtractor;
import com.jasper.invoice.application.document.port.PdfPageRenderer;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Component
@Primary
public class FallbackDocumentTextExtractor implements DocumentTextExtractor {

    private final PdfTextExtractor pdfTextExtractor;
    private final PdfPageRenderer pdfPageRenderer;
    private final ImageTextExtractor imageTextExtractor;

    public FallbackDocumentTextExtractor(
            PdfTextExtractor pdfTextExtractor,
            PdfPageRenderer pdfPageRenderer,
            ImageTextExtractor imageTextExtractor) {
        this.pdfTextExtractor = pdfTextExtractor;
        this.pdfPageRenderer = pdfPageRenderer;
        this.imageTextExtractor = imageTextExtractor;
    }

    @Override
    public String extract(InputStream inputStream) throws IOException {
        byte[] documentBytes = inputStream.readAllBytes();

        String text = pdfTextExtractor.extract(
                new ByteArrayInputStream(documentBytes));

        if (text != null && !text.isBlank()) {
            return text;
        }

        List<BufferedImage> pages = pdfPageRenderer.render(
                new ByteArrayInputStream(documentBytes));

        StringBuilder ocrText = new StringBuilder();

        for (BufferedImage page : pages) {
            String pageText = imageTextExtractor.extract(page);

            if (pageText != null && !pageText.isBlank()) {
                ocrText.append(pageText).append('\n');
            }
        }

        return ocrText.toString();
    }
}