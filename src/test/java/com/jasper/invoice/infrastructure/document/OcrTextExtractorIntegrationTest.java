package com.jasper.invoice.infrastructure.document;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class OcrTextExtractorIntegrationTest {

    @Autowired
    private OcrTextExtractor extractor;

    @Test
    void shouldExtractTextFromImage() throws Exception {
            BufferedImage image = ImageIO.read(
                    Objects.requireNonNull(getClass().getResourceAsStream("/ocr-test.png")));
            assertThat(image).isNotNull();

            String text = extractor.extract(image);

            assertThat(text).contains("INVOICE");
            assertThat(text).contains("Invoice Number");
    }
}