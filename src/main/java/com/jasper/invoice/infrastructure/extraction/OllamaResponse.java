package com.jasper.invoice.infrastructure.extraction;

public record OllamaResponse(
        String model,
        String response,
        boolean done,
        long load_duration,
        long prompt_eval_duration,
        int prompt_eval_count,
        long eval_duration,
        int eval_count
) {
}