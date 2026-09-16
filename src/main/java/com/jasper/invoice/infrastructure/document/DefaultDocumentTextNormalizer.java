package com.jasper.invoice.infrastructure.document;

import com.jasper.invoice.application.document.port.DocumentTextNormalizer;
import org.springframework.stereotype.Component;

@Component
public class DefaultDocumentTextNormalizer implements DocumentTextNormalizer {

    @Override
    public String normalize(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }

        return text
                .replace('\u00A0', ' ')
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .lines()
                .map(String::stripTrailing)
                .collect(java.util.stream.Collectors.joining("\n"))
                .replaceAll("\n{3,}", "\n\n")
                .strip();
    }
}