package org.kh.manju.llm;

import java.util.stream.Stream;

public interface LlmClient {
    String provider();

    ChatResult chat(ChatRequest request);

    StructuredResult structured(StructuredRequest request);

    Stream<ChatChunk> stream(ChatRequest request);
}
