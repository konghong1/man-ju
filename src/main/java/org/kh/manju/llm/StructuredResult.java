package org.kh.manju.llm;

import com.fasterxml.jackson.databind.JsonNode;

public record StructuredResult(
        JsonNode payload,
        int inputTokens,
        int outputTokens,
        long latencyMs
) {
}
