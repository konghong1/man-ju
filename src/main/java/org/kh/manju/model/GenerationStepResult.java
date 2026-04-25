package org.kh.manju.model;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;

public record GenerationStepResult(
        GenerationStep step,
        StepStatus status,
        JsonNode inputPayload,
        JsonNode outputPayload,
        String provider,
        String model,
        int inputTokens,
        int outputTokens,
        double costUsd,
        long latencyMs,
        int retries,
        String error,
        Instant startedAt,
        Instant endedAt
) {
}
