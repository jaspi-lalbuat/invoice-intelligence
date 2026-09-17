package com.jasper.invoice.infrastructure.document;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.InputStream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
@Tag("ocr")
public class FallbackDocumentTextExtractorIntegrationTest {
    @Autowired
    private FallbackDocumentTextExtractor extractor;

    @Test
    void shouldFallbackToOcrWhenPdfContainsNoText() throws Exception {
        try (InputStream inputStream =
                     getClass().getResourceAsStream("/scanned-invoice.pdf")) {

            assertThat(inputStream).isNotNull();

            String text = extractor.extract(inputStream);

            assertThat(text).contains("INVOICE");
            assertThat(text).contains("Invoice Number");
            assertThat(text).contains("INV-12345");
        }
    }
}
