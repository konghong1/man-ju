package org.kh.manju.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.stream.Stream;

public abstract class SimulatedLlmClient implements LlmClient {

    private final ObjectMapper objectMapper;

    protected SimulatedLlmClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public ChatResult chat(ChatRequest request) {
        String text = "[%s:%s] %s"
                .formatted(provider(), request.model(), request.userPrompt());
        return new ChatResult(
                text,
                estimateTokens(request.systemPrompt() + request.userPrompt()),
                estimateTokens(text),
                18
        );
    }

    @Override
    public StructuredResult structured(StructuredRequest request) {
        JsonNode payload = objectMapper.valueToTree(Map.of(
                "provider", provider(),
                "model", request.model(),
                "instruction", request.instruction(),
                "schema", request.schema()
        ));
        return new StructuredResult(
                payload,
                estimateTokens(request.instruction() + request.schema()),
                estimateTokens(payload.toString()),
                20
        );
    }

    @Override
    public Stream<ChatChunk> stream(ChatRequest request) {
        return Stream.of(request.userPrompt().split("\\s+"))
                .filter(token -> !token.isBlank())
                .map(ChatChunk::new);
    }

    private int estimateTokens(String text) {
        return Math.max(10, text.length() / 4);
    }
}
