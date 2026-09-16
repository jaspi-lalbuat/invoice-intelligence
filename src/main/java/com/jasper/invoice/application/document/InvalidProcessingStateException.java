package com.jasper.invoice.application.document;

public class InvalidProcessingStateException
        extends IllegalStateException {

    public InvalidProcessingStateException(
            String message) {
        super(message);
    }
}