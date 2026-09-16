package com.jasper.invoice.application.document.port;

import java.io.IOException;
import java.io.InputStream;

public interface DocumentTextExtractor {

    String extract(InputStream inputStream) throws IOException;
}