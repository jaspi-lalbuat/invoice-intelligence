package com.jasper.invoice.domain.model;

public record Party(
        String name,
        String address,
        String taxId
) {}