package com.jasper.invoice.application.document.port;

import java.awt.image.BufferedImage;

public interface ImageTextExtractor {

    String extract(BufferedImage image);
}