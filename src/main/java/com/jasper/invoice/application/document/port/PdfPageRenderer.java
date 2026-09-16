package com.jasper.invoice.application.document.port;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public interface PdfPageRenderer {
    List<BufferedImage> render(InputStream inputStream) throws IOException;
}