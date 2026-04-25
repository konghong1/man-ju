package org.kh.manju.llm;

public record StructuredRequest(
        String instruction,
        String schema,
        String model
) {
}
