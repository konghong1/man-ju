package org.kh.manju.llm;

public record ChatRequest(
        String systemPrompt,
        String userPrompt,
        String model,
        double temperature,
        int maxTokens
) {
}
