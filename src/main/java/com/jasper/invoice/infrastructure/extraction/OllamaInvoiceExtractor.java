package com.jasper.invoice.infrastructure.extraction;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jasper.invoice.application.document.port.InvoiceExtractor;
import com.jasper.invoice.domain.model.Invoice;
import com.jasper.invoice.domain.model.InvoiceLineItem;
import com.jasper.invoice.domain.model.Party;
import com.jasper.invoice.domain.model.TaxBreakdown;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.util.Map;

@Slf4j
@Component
public class OllamaInvoiceExtractor implements InvoiceExtractor {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String model;

    public OllamaInvoiceExtractor(
            RestClient.Builder restClientBuilder,
            OllamaProperties properties,
            ObjectMapper objectMapper) {

        JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory(
                        HttpClient.newBuilder()
                                .connectTimeout(properties.requestTimeout())
                                .build()
                );
        requestFactory.setReadTimeout(properties.requestTimeout());

        this.restClient = restClientBuilder
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory)
                .build();

        this.objectMapper = objectMapper;
        this.model = properties.model();
    }

    @Override
    public Invoice extract(String documentText) {
        long startNanos = System.nanoTime();
        String prompt = buildPrompt(documentText);

        Map<String, Object> request = Map.of(
                "model", this.model,
                "stream", false,
                "format", "json",
                "prompt", prompt
        );

        OllamaResponse ollamaResponse = restClient.post()
                .uri("/api/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(OllamaResponse.class);

        if (ollamaResponse == null || ollamaResponse.response() == null) {
            throw new IllegalStateException("Invalid response from Ollama");
        }

        long durationMillis =
                (System.nanoTime() - startNanos) / 1_000_000;

        log.info(
                "Invoice extraction completed: durationMs={}, loadDurationMs={}, promptEvalDurationMs={}, evalDurationMs={}, promptTokens={}, evalTokens={}",
                durationMillis,
                ollamaResponse.load_duration() / 1_000_000,
                ollamaResponse.prompt_eval_duration() / 1_000_000,
                ollamaResponse.eval_duration() / 1_000_000,
                ollamaResponse.prompt_eval_count(),
                ollamaResponse.eval_count()
        );

        String json = ollamaResponse.response();

        try {
            OllamaInvoiceResponse response =
                    objectMapper.readValue(json, OllamaInvoiceResponse.class);
            return mapToDomain(response);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to parse invoice extraction response", e
            );
        }
    }

    private String buildPrompt(String documentText) {
        return """
                You are an invoice data extraction engine.

                Extract ONLY information explicitly present in the invoice text.

                Rules:
                1. Return exactly one valid JSON object.
                2. Do not output markdown, explanations, reasoning, or any other text.
                3. Do not calculate, infer, correct, or modify values.
                4. If a field is not explicitly present, use null.
                5. Currency must be represented as an ISO 4217 code such as INR.
                6. Dates must use YYYY-MM-DD.
                7. Numbers must be JSON numbers, not strings.
                8. For line items, statedTotal means a total explicitly printed for that line.
                   Do not calculate it from quantity and unit price.

                JSON schema:

                {
                  "invoiceNumber": null,
                  "invoiceDate": null,
                  "dueDate": null,
                  "currency": null,
                  "supplier": {
                    "name": null,
                    "address": null,
                    "taxId": null
                  },
                  "customer": {
                    "name": null,
                    "address": null,
                    "taxId": null
                  },
                  "lineItems": [
                    {
                      "description": null,
                      "quantity": null,
                      "unitPrice": null,
                      "taxRate": null,
                      "statedTotal": null
                    }
                  ],
                  "subtotal": null,
                  "taxes": {
                    "cgst": null,
                    "sgst": null,
                    "igst": null
                  },
                  "discount": null,
                  "total": null
                }

                INVOICE TEXT:
                %s
                """.formatted(documentText);
    }

    Invoice mapToDomain(OllamaInvoiceResponse response) {
        return new Invoice(
                response.invoiceNumber(),
                response.invoiceDate(),
                response.dueDate(),
                response.currency(),
                mapParty(response.supplier()),
                mapParty(response.customer()),
                response.lineItems().stream()
                        .map(this::mapLineItem)
                        .toList(),
                response.subtotal(),
                mapTaxes(response.taxes()),
                response.discount(),
                response.total()
        );
    }

    private Party mapParty(OllamaInvoiceResponse.PartyResponse party) {
        if (party == null) {
            return null;
        }

        return new Party(
                party.name(),
                party.address(),
                party.taxId()
        );
    }

    private TaxBreakdown mapTaxes(
            OllamaInvoiceResponse.TaxBreakdownResponse taxes) {

        if (taxes == null) {
            return null;
        }

        return new TaxBreakdown(
                taxes.cgst(),
                taxes.sgst(),
                taxes.igst()
        );
    }

    private InvoiceLineItem mapLineItem(
            OllamaInvoiceResponse.InvoiceLineItemResponse item) {

        return new InvoiceLineItem(
                item.description(),
                item.quantity(),
                item.unitPrice(),
                item.taxRate(),
                item.statedTotal()
        );
    }
}
