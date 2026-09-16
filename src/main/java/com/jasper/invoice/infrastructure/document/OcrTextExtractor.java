package com.jasper.invoice.infrastructure.document;

import com.jasper.invoice.application.document.port.ImageTextExtractor;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Component
public class OcrTextExtractor implements ImageTextExtractor {

    private final Tesseract tesseract;

    public OcrTextExtractor(TesseractProperties properties) {
        this.tesseract = new Tesseract();
        this.tesseract.setDatapath(properties.datapath());
        this.tesseract.setLanguage(properties.language());
    }

    @Override
    public String extract(BufferedImage image) {
        try {
            return tesseract.doOCR(normalizeImage(image));
        } catch (TesseractException | IOException e) {
            throw new IllegalStateException("OCR extraction failed", e);
        }
    }

    private BufferedImage normalizeImage(BufferedImage image) throws IOException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", outputStream);

            try (ByteArrayInputStream inputStream =
                         new ByteArrayInputStream(outputStream.toByteArray())) {
                return ImageIO.read(inputStream);
            }
        }
    }
}