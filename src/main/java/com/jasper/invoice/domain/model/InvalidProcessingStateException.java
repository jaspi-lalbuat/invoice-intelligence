package com.jasper.invoice.domain.model;

public class InvalidProcessingStateException extends RuntimeException {

    public InvalidProcessingStateException(String message) {
        super(message);
    }
}
