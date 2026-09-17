package com.jasper.invoice.infrastructure.persistence;

import com.jasper.invoice.application.document.port.DocumentStorage;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Value;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Component

public class LocalDocumentStorage implements DocumentStorage {

    private final Path storageDirectory;

    public LocalDocumentStorage(
            @Value("${document.storage.path:storage/documents}") String storagePath
    ) {
        this.storageDirectory = Path.of(storagePath);
    }

    @Override
    public String store(MultipartFile file) throws IOException {

        Files.createDirectories(storageDirectory);

        String reference = UUID.randomUUID().toString();

        Path destination = storageDirectory.resolve(reference);

        file.transferTo(destination);

        return reference;
    }

    @Override
    public InputStream load(String documentReference) throws IOException {

        Path documentPath =
                storageDirectory.resolve(documentReference);

        if (!Files.exists(documentPath)) {
            throw new FileNotFoundException(
                    "Document not found: " + documentReference
            );
        }

        return Files.newInputStream(documentPath);
    }

    @Override
    public void delete(String documentReference) throws IOException {

        Path documentPath =
                storageDirectory.resolve(documentReference);

        Files.deleteIfExists(documentPath);
    }
}