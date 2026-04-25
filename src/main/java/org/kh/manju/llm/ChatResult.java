package org.kh.manju.llm;

public record ChatResult(
        String text,
        int inputTokens,
        int outputTokens,
        long latencyMs
) {
}
