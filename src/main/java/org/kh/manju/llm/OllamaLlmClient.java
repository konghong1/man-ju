package org.kh.manju.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class OllamaLlmClient extends SimulatedLlmClient {

    public OllamaLlmClient(ObjectMapper objectMapper) {
        super(objectMapper);
    }

    @Override
    public String provider() {
        return "ollama";
    }
}
