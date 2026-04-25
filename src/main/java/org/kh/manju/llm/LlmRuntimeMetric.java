package org.kh.manju.llm;

import org.kh.manju.model.GenerationStep;

public record LlmRuntimeMetric(
        String provider,
        String model,
        GenerationStep step,
        int requests,
        double successRate,
        long p95LatencyMs,
        double avgCostUsd,
        int totalTokens
) {
}
