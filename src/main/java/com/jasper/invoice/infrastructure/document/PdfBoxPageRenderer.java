package com.jasper.invoice.infrastructure.document;

import com.jasper.invoice.application.document.port.PdfPageRenderer;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Component
public class PdfBoxPageRenderer implements PdfPageRenderer {

    private static final float DPI = 200;

    @Override
    public List<BufferedImage> render(InputStream inputStream) throws IOException {
        byte[] documentBytes = inputStream.readAllBytes();

        try (PDDocument document = Loader.loadPDF(documentBytes)) {
            PDFRenderer renderer = new PDFRenderer(document);

            List<BufferedImage> images = new ArrayList<>();

            for (int page = 0; page < document.getNumberOfPages(); page++) {
                images.add(renderer.renderImageWithDPI(page, DPI));
            }

            return images;
        }
    }
}