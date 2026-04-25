package org.kh.manju.llm;

import org.kh.manju.model.GenerationStep;

public record LlmAlert(
        String severity,
        String provider,
        String model,
        GenerationStep step,
        String message
) {
}
