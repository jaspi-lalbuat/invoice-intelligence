package com.jasper.invoice.application.document.port;

import java.io.IOException;
import java.io.InputStream;

public interface DocumentValidator {

    void validate(InputStream inputStream) throws IOException;
}