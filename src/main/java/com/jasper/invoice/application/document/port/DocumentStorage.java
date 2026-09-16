package com.jasper.invoice.application.document.port;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

public interface DocumentStorage {

    String store(MultipartFile file) throws IOException;
    InputStream load(String documentReference) throws IOException;
    void delete(String documentReference) throws IOException;
}